// @ts-ignore no typescript package available
import {clipboard, ipcRenderer} from 'electron';
import ElectronOverwolfInterface from './electron-overwolf-interface';
import fs from 'fs';
import path from 'path';
import os from 'os';
import {handleAction} from '@/core/protocol/protocolActions';
import log from 'electron-log';
import {createLogger} from '@/core/logger';
import {computeArch, computeOs, jreLocation, parseArgs} from '@/utils/interface/electron-helpers';
import {execSync} from 'child_process';

export type Arg = string | {
  key?: string;
  value: string;
  filter: { 
    os?: string[] | string;
    arch?: string[] | string;
  }
}

export type MetaData = {
  appVersion: string;
  commit: string;
  branch: string;
  released: number;
  runtime: {
    version: string;
    jar: string;
    env: Arg[];
    jvmArgs: Arg[];
  };
}

const fallbackMetaData: MetaData = {
  appVersion: "Unknown",
  commit: "Unknown",
  branch: "release",
  released: new Date().getTime(),
  runtime: {
    version: "21", // If we're using this, something has gone wrong
    jar: "launcher.jar", // If we're using this, something has gone wrong
    env: [],
    jvmArgs: []
  }
}

const eLogger = createLogger("platform/electron.ts");
const electronFrontendLogFile = path.join(getAppHome(), 'logs', 'ftb-app-frontend.log');
if (fs.existsSync(electronFrontendLogFile)) {
  try {
    fs.writeFileSync(electronFrontendLogFile, '')
  } catch (e) {
    eLogger.error("Failed to clear electron frontend log file", e)
  }
}
log.transports.file.resolvePath = () =>
  electronFrontendLogFile;
Object.assign(console, log.functions);

const resourcesPath = process.resourcesPath

eLogger.info("Resources path", resourcesPath)

const metaFilePath = path.join(resourcesPath, "meta.json");
const metaData = process.env.NODE_ENV === "development" ? fallbackMetaData : JSON.parse(fs.readFileSync(metaFilePath, 'utf-8'));

let licensesData: any = {};
let javaLicensesData: any = {};

try {
  licensesData = JSON.parse(fs.readFileSync(path.join(resourcesPath, "licenses.json"), 'utf-8'));
  javaLicensesData = JSON.parse(fs.readFileSync(path.join(resourcesPath, "java-licenses.json"), 'utf-8'));
} catch (e) {
  eLogger.error("Failed to load licenses", e);
}

eLogger.info("Meta data", metaData)

function getAppHome() {
  if (os.platform() === "darwin") {
    return path.join(os.homedir(), 'Library', 'Application Support', '.ftba');
  } else if (os.platform() === "win32") {
    return path.join(os.homedir(), 'AppData', 'Local', '.ftba');
  } else {
    return path.join(os.homedir(), '.ftba');
  }
}

const Electron: ElectronOverwolfInterface = {
  config: {
    version: metaData.appVersion ?? 'Missing Version File',
    commit: metaData.commit ?? 'Missing Version File',
    dateCompiled: metaData.released ?? new Date().getTime(),
    branch: metaData.branch ?? 'release'
  },

  // Tools
  utils: {
    openUrl(url: string) {
      ipcRenderer.send('openLink', url);
    },

    getOsArch() {
      return os.arch();
    },

    async getPlatformVersion() {
      return process.versions.electron;
    },
    
    crypto: {
      randomUUID(): string {
        return (crypto as any).randomUUID();
      }
    },
    
    openDevTools() {
      ipcRenderer.send('openDevTools');
    },
  },

  // Actions
  actions: {
    openModpack(payload: { name: string; id: string }) {
      ipcRenderer.send('openModpack', { name: payload.name, id: payload.id });
    },

    openFriends() {
      ipcRenderer.send('showFriends');
    },

    // Obviously do nothing
    changeExitOverwolfSetting() {},    

    onAppReady() {
      eLogger.debug("Interface has been told the app is ready")
      ipcRenderer.send('appReady');
    },

    uploadClientLogs() {},

    restartApp() {
      eLogger.debug("Restarting app")
      // Restart the electron app
      ipcRenderer.send('restartApp');
    }
  },

  // Clipboard
  cb: {
    copy(e: string) {
      clipboard.writeText(e);
    },
    paste(): string {
      return clipboard.readText();
    },
  },

  // Frame / Chrome / Window / What ever you want to call it
  frame: {
    close() {
      ipcRenderer.send('windowControls', { action: 'close' });
    },
    min() {
      ipcRenderer.send('windowControls', { action: 'minimize' });
    },
    max() {
      ipcRenderer.send('windowControls', { action: 'maximize' });
    },

    quit() {
      ipcRenderer.send('quit_app');
    },

    expandWindow() {
      ipcRenderer.send('expandMeScotty', { width: 800 });
    },
    collapseWindow() {
      ipcRenderer.send('expandMeScotty', { width: 300 });
    },

    // we don't need this on electron because it's not silly
    async getWindowId() {
      return ""
    },
    handleDrag() {},
    setSystemWindowStyle(enabled) {
      ipcRenderer.invoke('setSystemWindowStyle', enabled);
    }
  },

  // IO
  io: {
    selectFolderDialog(startPath, cb) {
      ipcRenderer
        .invoke('selectFolder', startPath)
        .then((dir) => cb(dir))
        .catch((e) => {
          eLogger.warn("Failed to select folder from the system", e)
          cb(null)
        });
    },

    selectFileDialog(cb) {
      ipcRenderer
        .invoke('selectFile')
        .then((dir) => {
          cb(dir);
        })
        .catch((e) => {
          eLogger.warn("Failed to select file from the system", e)
          cb(null)
        });
    },
    
    openFinder(path: string): Promise<boolean> {
      return ipcRenderer.invoke('openFinder', path);
    },
    
    getLocalAppData() {
      return path.join(os.homedir(), "AppData", "Local"); 
    }
  },
  
  app: {
    async appHome(): Promise<string> {
      return getAppHome();
    },
    async appSettings() {
      const settingsPath = getAppHome() + "/bin/settings.json";
      if (!fs.existsSync(settingsPath)) {
        return null
      }

      return JSON.parse(fs.readFileSync(settingsPath, 'utf-8'));
    },
    async appData(): Promise<string> {
      return path.join(getAppHome(), 'bin');
    },
    async appRuntimes(): Promise<string> {
      return path.join(getAppHome(), 'runtime');
    },
    async runtimeAvailable(): Promise<boolean> {
      const appPath = getAppHome();
      return fs.existsSync(jreLocation(appPath));
    },
    async installApp(onStageChange: (stage: string) => void, onUpdate: (data: any) => void, isUpdate = false) {
      const fsLogger = new FSLogger("ftb-app-installer");
      
      const logAndUpdate = (message: string) => {
        fsLogger.log(message);
        onStageChange(message);
      }
      
      const logToBoth = (message: string, normalLogger: any, ...args: any) => {
        const argsList = [...args];
        fsLogger.log(message + argsList.length ? " [" + JSON.stringify(argsList) + "]" : "");
        normalLogger(message, ...args);
      }
      
      logAndUpdate("Locating Java");

      /**
       * Procedure:
       * - Figure out the system architecture and platform type (mac, windows, linux)
       * - Locate the java version from the adoptium api
       * - Download the java version
       * - Extract the tar or the zip
       * - Move the java version to the runtime folder
       * - Ensure the java version is executable and works (run java -version)
       * - If it doesn't work, try again
       * - If it still doesn't work, throw an error
       * - If it does work, continue
       * - Try and start the backend (Call to different method procedure)
       * - If it fails, try again
       * - If it fails more than 3 times, throw an error
       */
      
      const platformData = {
        arch: os.arch(),
        platform: os.platform(),
      };

      const ourOs = computeOs(platformData.platform);
      const ourArch = computeArch(platformData.arch)

      const javaVersion = metaData.runtime.version;
      
      // Attempt to get the java
      const url = `https://api.adoptium.net/v3/assets/latest/${javaVersion}/hotspot?architecture=${ourArch}&image_type=jre&os=${ourOs}`;

      logToBoth("Downloading java from", eLogger.log, url);
      logAndUpdate("Downloading Java")
      
      let adopiumRes;
      try {
        adopiumRes = await retryAttempt(async () => {
          const adopiumRequest = await fetch(url);
          return await adopiumRequest.json();
        });
      } catch (e) {
        logToBoth("Failed to download java", eLogger.error, e)
        throw throwCustomError("Failed to download java", "We've not been able to download Java, this could be because of networking issues. Please ensure you can access https://adoptium.net/")
      }
      
      const binary = adopiumRes.find((e: any) => e.binary)?.binary;
      if (!binary) {
        logToBoth("Failed to find java binary", eLogger.error, adopiumRes)
        throw throwCustomError("Failed to find java binary", "We've not been able to find a Java binary for you system. Please ensure you are using a supported system.")
      }
      
      const link = binary.package.link;

      const appPath = getAppHome();
      const runtimePath = `${appPath}/runtime`;
      
      if (isUpdate) {
        logAndUpdate("Removing old runtime")
        if (fs.existsSync(runtimePath)) {
          try {
            fs.rmSync(runtimePath, {
              recursive: true
            });
          } catch (e) {
            logToBoth("Failed to remove runtime folder", eLogger.error, e)
            throw throwCustomError("Failed to remove runtime folder", "We've not been able to remove the runtime folder. Please ensure the app has the correct permissions to remove folders.")
          }
        }
      }

      logAndUpdate("Creating runtime folder")
      try {
        if (!fs.existsSync(runtimePath)) {
          fs.mkdirSync(runtimePath, {
            recursive: true
          });
        }
      } catch (e) {
        logToBoth("Failed to create runtime folder", eLogger.error, e)
        throw throwCustomError("Failed to create runtime folder", "We've not been able to create the runtime folder. Please ensure the app has the correct permissions to create folders.")
      }
      
      const isWindows = os.platform() === "win32";
      const jreDownloadName = isWindows ? `jre.zip` : `jre.tar.gz`;
      logAndUpdate("Downloading Java from Adoptium")
      try {
        await ipcRenderer.invoke("downloadFile", {
          url: link,
          path: path.join(runtimePath, jreDownloadName)
        });
      } catch (e) {
        logToBoth("Failed to download java from endpoint", eLogger.error, e)
        throw throwCustomError("Failed to download java from endpoint", "We've not been able to download Java, this could be because of networking issues. Please ensure you can access https://adoptium.net/")
      }

      logAndUpdate("Extracting Java")
      try {
        await ipcRenderer.invoke("extractFile", {
          input: path.join(runtimePath, jreDownloadName),
          output: path.join(runtimePath, `jre`)
        });
      } catch (e) {
        logToBoth("Failed to extract java", eLogger.error, e)
        throw throwCustomError("Failed to extract java", "We've not been able to extract Java... We can't recover from this.")
      }
      
      // Assuming this worked
      // Does the jre/jdk-* folder exist?
      logAndUpdate("Moving Java to the correct location")
      const jreFolder = fs.readdirSync(path.join(runtimePath, "jre")).find(e => e.startsWith("jdk-"));
      if (jreFolder == null) {
        logToBoth("Failed to find java folder", eLogger.error)
        throw throwCustomError("Failed to find java folder", "We've not been able to find the Java folder. We can't recover from this.")
      }
      
      // Move all of the folders contents to the runtime folder
      // Then delete the jre folder
      const jrePath = path.join(runtimePath, "jre", jreFolder);
      const jreFiles = fs.readdirSync(jrePath);
      jreFiles.forEach(e => {
        fs.renameSync(path.join(jrePath, e), path.join(runtimePath, e));
      });

      logAndUpdate("Cleaning up")
      try {
        // It's not fatal if this fails
        fs.rmdirSync(jrePath);
        fs.rmdirSync(path.join(runtimePath, 'jre'))
        fs.rmSync(path.join(runtimePath, jreDownloadName));
      } catch (e) {
        logToBoth("Failed to clean up", eLogger.error, e)
        // Ignore
      }

      logAndUpdate("Finishing up")
      // Touch a file to note down what java version we have
      try {
        fs.writeFileSync(path.join(runtimePath, ".java-version"), javaVersion);
      } catch (e) {
        logToBoth("Failed to write java version file", eLogger.error, e)
        throw throwCustomError("Failed to write java version file", "We've not been able to write the java version file. We can't recover from this.")
      }

      logAndUpdate("Checking Java works")
      // Ensure the java version is executable and works
      const jreExecPath = jreLocation(appPath);
      if (!fs.existsSync(jreExecPath)) {
        logToBoth("Failed to find java executable", eLogger.error)
        throw throwCustomError("Failed to find java executable", "We've not been able to find the java executable. We can't recover from this.")
      }
      
      // Run the java --version command and ensure we get the correct exit code
      try {
        execSync(`"${jreExecPath}" --version`, {
          stdio: 'ignore'
        });
      } catch (e) {
        logToBoth("Failed to run java --version", eLogger.error)
        throw throwCustomError("Failed to run java --version", "We've not been able to run the java --version command. We can't recover from this.")
      }

      logAndUpdate("Starting subprocess")
    },
    async updateApp(onStageChange: (stage: string) => void, onUpdate: (data: any) => void) {
      // Get the runtime
      const appPath = getAppHome();
      const runtimePath = `${appPath}/runtime`;
      
      if (!fs.existsSync(`${runtimePath}/.java-version`)) {
        // How did we get here? Just install it
        return await this.installApp(onStageChange, onUpdate, false);
      }
      
      // Get the original java version
      const javaVersion = fs.readFileSync(`${runtimePath}/.java-version`, 'utf-8');
      
      if (metaData.runtime.version === javaVersion) {
        // We don't need to update
        return;
      }
      
      // We need to update
      return await this.installApp(onStageChange, onUpdate, true);
    },
    async startSubprocess() {
      // Get the runtime
      const appPath = getAppHome();
      const runtimePath = `${appPath}/runtime`;
      
      if (!fs.existsSync(`${runtimePath}/.java-version`)) {
        throw new CustomError("Failed to find java version file", "We've not been able to find the java version file. We can't recover from this.");
      }
      
      const javaVersion = fs.readFileSync(`${runtimePath}/.java-version`, 'utf-8');
      if (!javaVersion) {
        throw new CustomError("Failed to read java version file", "We've not been able to read the java version file. We can't recover from this.");
      }
      
      if (javaVersion !== metaData.runtime.version) {
        // Looks like we need to try and update the java version
        return false;
      }
      
      // Start the subprocess
      const jreExecPath = jreLocation(appPath);
      if (!fs.existsSync(jreExecPath)) {
        throw new Error("Failed to find java executable");
      }
      
      const envVars = parseArgs(metaData.runtime.env);
      const jvmArgs = parseArgs(metaData.runtime.jvmArgs);
      
      const jarName = metaData.runtime.jar;
      
      try {
        return await retryAttempt(async () => {
          const {port, secret} = await ipcRenderer.invoke("startSubprocess", {
            javaPath: jreExecPath,
            args: [
              ...jvmArgs,
              "-jar",
              `${resourcesPath}/${jarName}`,
            ],
            env: envVars
          });

          if (port && secret) {
            return {
              port,
              secret
            }
          }

          throw new CustomError("Failed to start subprocess", "We've not been able to start the subprocess. We can't recover from this.")
        }, 20);
      } catch (e) {
        throw new CustomError("Failed to start subprocess after 20 attempts", "We've not been able to start the subprocess. We can't recover from this.")
      }
    },
    getLicenses() {
      return {
        javascript: licensesData,
        java: javaLicensesData
      }
    },
    cpm: {
      async required() {
        // Required for OW Ads 
        return await ipcRenderer.invoke("ow:cpm:is_required");
      },
      async openWindow(arg = "purposes") {
        // Required for OW Ads
        await ipcRenderer.invoke("ow:cpm:open_window", arg);
      },
      async isFirstLaunch() {
        return !fs.existsSync(path.join(getAppHome(), "bin", ".first-launch"));
      },
      async setFirstLaunched() {
        // Create parents
        if (!fs.existsSync(path.join(getAppHome(), "bin"))) {
          fs.mkdirSync(path.join(getAppHome(), "bin"), {
            recursive: true
          });
        }
        
        fs.writeFileSync(path.join(getAppHome(), "bin", ".first-launch"), "");
      }
    }
  },
  
  setupApp() {
    eLogger.debug("Setting up the app from the interface on electron")
    
    ipcRenderer.on('parseProtocolURL', (event, data) => {
      handleAction(data);
      // TODO: (M#01) Reimplement missing protocol systems
      // let protocolURL = data;
      // if (protocolURL === undefined) {
      //   return;
      // }
      // protocolURL = protocolURL.substring(6, protocolURL.length);
      // const parts = protocolURL.split('/');
      // const command = parts[0];
      // const args = parts.slice(1, parts.length);
      // if (command === 'modpack') {
      //   if (args.length === 0) {
      //     return;
      //   }
      //   logVerbose(store.state, 'Received modpack protocol message', args);
      //   const modpackID = args[0];
      //   if (args.length === 1) {
      //     // Navigate to page for modpack
      //     logVerbose(store.state, 'Navigating to page for modpack', modpackID);
      //     router.push({ name: 'modpackpage', query: { modpackid: modpackID } });
      //   } else if (args.length === 2) {
      //     if (args[1] === 'install') {
      //       // Popup install for modpack
      //       logVerbose(store.state, 'Popping up install for modpack', modpackID);
      //       router.push({ name: 'modpackpage', query: { modpackid: modpackID, showInstall: 'true' } });
      //     }
      //   } else if (args.length === 3) {
      //     if (args[2] === 'install') {
      //       // Popup install for modpack with version default selected
      //       router.push({
      //         name: 'modpackpage',
      //         query: { modpackid: modpackID, showInstall: 'true', version: args[1] },
      //       });
      //     }
      //   }
      // }
    });
  },
};

/**
 * Retries a promise a number of times with a delay
 * @param fn
 * @param attempts
 */
async function retryAttempt<T>(fn: () => Promise<T>, attempts = 3): Promise<T> {
  let attempt = 0;
  while (attempt < attempts) {
    try {
      return await fn();
    } catch (e) {
      await new Promise((resolve) => setTimeout(resolve, 1000)); // Wait for a second
      attempt++;
    }
  }

  throw new Error("Failed to retry attempt")
}

function throwCustomError(message: string, customMessage: string) {
  eLogger.error(message);
  throw new CustomError(message, customMessage);
}

class CustomError extends Error {
  customMessage: string;
  
  constructor(message: string, customMessage: string) {
    super(message);
    this.name = "CustomError";
    this.customMessage = customMessage;
  }
}

export default Electron;

/**
 * Avoid use unless absolutely need!
 */
class FSLogger {
  path: string;
  parent: string;
  
  constructor(name: string) {
    this.parent = path.join(getAppHome(), 'logs');
    this.path = path.join(this.parent, `${name}.log`);
    
    this.ensureParentExists();
  }
  
  log(message: string) {
    try {
      this.ensureParentExists();
      
      fs.appendFileSync(this.path, `[${new Date().toISOString()}] - ${message}\n`, {
        encoding: 'utf-8',
      });
    } catch (e) {
      console.error("Failed to log message", e);
      console.debug("Original message", message);
    }
  }
  
  private ensureParentExists() {
    try {
      if (!fs.existsSync(this.parent)) {
        fs.mkdirSync(this.parent, {
          recursive: true
        });
      }
    } catch (e) {
      console.error("Failed to create log directory", e);
    }
  }
}