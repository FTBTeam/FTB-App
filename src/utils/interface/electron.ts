// @ts-ignore no typescript package available
import {clipboard, ipcRenderer} from 'electron';
import ElectronOverwolfInterface from './electron-overwolf-interface';
import fs from 'fs';
import path from 'path';
import EventEmitter from 'events';
import http from 'http';
import os from 'os';
import {handleAction} from '@/core/protocol/protocolActions';
import platform from '@/utils/interface/electron-overwolf';
import {emitter} from '@/utils';
import {AuthenticationCredentialsPayload} from '@/core/@types/authentication.types';
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
log.transports.file.resolvePath = (vars, message) =>
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

class MiniWebServer extends EventEmitter {
  server: http.Server | null = null;
  timeoutRef: NodeJS.Timeout | null = null;
  closing = false;

  open() {
    this.closing = false;
    if (this.server == null) {
      this.server = http.createServer((req: any, res: any) => {
        let body = '';
        req.on('data', (chunk: any) => {
          body += chunk;
        });

        req.on('end', () => {
          if (!body) {
            res.end();
            return;
          }

          const jsonResponse = JSON.parse(body);
          if (jsonResponse == null) {
            eLogger.debug('Failed to parse json response');
            res.end();
            this.close();
            return;
          }

          // HACKS Hijank the old flow
          if (jsonResponse.key) {
            this.emit('response', jsonResponse);
            res.write('success');
            res.end();
            this.close();
            return;
          }

          const { token, 'app-auth': appAuth } = jsonResponse;
          if (token == null || appAuth == null) {
            eLogger.error('Failed to parse token or appAuth');
            return;
          }

          this.emit('response', jsonResponse);
          res.write('success');
          res.end();
          this.close();
        });
      });

      this.server.listen(7755, () => {
        eLogger.debug('MiniWebServer listening on 7755');
        this.emit('open');
      });

      this.closeAfterFive();
    } else {
      this.emit('open', false);
    }
  }

  closeAfterFive() {
    if (this.timeoutRef != null) {
      clearTimeout(this.timeoutRef);
    }

    this.timeoutRef = setTimeout(async () => {
      this.emit('timeout');
      await this.close();
    }, 1000 * 60 * 5);
  }

  async close() {
    if (!this.server || this.closing) {
      return;
    }

    this.closing = true;
    return new Promise((resolve) => {
      this.server?.close(() => {
        this.server = null;
        if (this.timeoutRef != null) {
          clearTimeout(this.timeoutRef);
        }

        this.closing = false;
        this.emit('close');
        resolve(true);
      });
    });
  }
}

let miniServers: MiniWebServer[] = [];

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
    async openMsAuth() {
      eLogger.debug("Opening ms auth page and starting server")
      platform.get.utils.openUrl("https://msauth.feed-the-beast.com");

      const mini = new MiniWebServer();
      miniServers.push(mini);
      const result: any = await new Promise((resolve, reject) => {
        mini.open();
        mini.on('response', (data: { token: string; 'app-auth': string }) => {
          eLogger.debug("Received response from mini web server")
          resolve(data);
        });

        mini.on('close', () => {
          eLogger.debug("Closing mini web server")
          resolve(null);
        });
      });

      eLogger.debug("Finished mini web server flow");
      emitter.emit("authentication.callback", result?.key ? result : undefined);
    },
    
    emitAuthenticationUpdate(credentials?: AuthenticationCredentialsPayload) {
      emitter.emit("authentication.callback", credentials)
    },

    closeWebservers() {
      miniServers.forEach(e => e.close())
      miniServers = [];
    },

    openModpack(payload: { name: string; id: string }) {
      ipcRenderer.send('openModpack', { name: payload.name, id: payload.id });
    },

    openFriends() {
      ipcRenderer.send('showFriends');
    },
    
    async openLogin(cb: (data: { token: string; 'app-auth': string }) => void) {
      // TODO: (legacy) Fix soon plz
      platform.get.utils.openUrl("https://minetogether.io/api/login?redirect=http://localhost:7755")
      
      const mini = new MiniWebServer();
      miniServers.push(mini);
      const result: any = await new Promise((resolve, reject) => {
        mini.open();
        mini.on('response', (data: { token: string; 'app-auth': string }) => {
          resolve(data);
        });

        mini.on('close', () => {
          resolve(null);
        });
      });
      
      mini.close().catch(e => eLogger.error("Failed to close the miniserver", e))
      cb(result);
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
      onStageChange("Locating Java");

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
      
      eLogger.log("Downloading java from", url)
      onStageChange("Downloading Java")
      
      let adopiumRes;
      try {
        adopiumRes = await retryAttempt(async () => {
          const adopiumRequest = await fetch(url);
          return await adopiumRequest.json();
        });
      } catch (e) {
        eLogger.error("Failed to download java", e)
        throw throwCustomError("Failed to download java", "We've not been able to download Java, this could be because of networking issues. Please ensure you can access https://adoptium.net/")
      }
      
      const binary = adopiumRes.find((e: any) => e.binary)?.binary;
      if (!binary) {
        throw throwCustomError("Failed to find java binary", "We've not been able to find a Java binary for you system. Please ensure you are using a supported system.")
      }
      
      const link = binary.package.link;

      const appPath = getAppHome();
      const runtimePath = `${appPath}/runtime`;
      
      if (isUpdate) {
        onStageChange("Removing old runtime")
        if (fs.existsSync(runtimePath)) {
          try {
            fs.rmSync(runtimePath, {
              recursive: true
            });
          } catch (e) {
            throw throwCustomError("Failed to remove runtime folder", "We've not been able to remove the runtime folder. Please ensure the app has the correct permissions to remove folders.")
          }
        }
      }

      onStageChange("Creating runtime folder")
      try {
        if (!fs.existsSync(runtimePath)) {
          fs.mkdirSync(runtimePath, {
            recursive: true
          });
        }
      } catch (e) {
        throw throwCustomError("Failed to create runtime folder", "We've not been able to create the runtime folder. Please ensure the app has the correct permissions to create folders.")
      }
      
      const isWindows = os.platform() === "win32";
      const jreDownloadName = isWindows ? `jre.zip` : `jre.tar.gz`;
      onStageChange("Downloading Java from Adoptium")
      try {
        await ipcRenderer.invoke("downloadFile", {
          url: link,
          path: path.join(runtimePath, jreDownloadName)
        });
      } catch (e) {
        throw throwCustomError("Failed to download java from endpoint", "We've not been able to download Java, this could be because of networking issues. Please ensure you can access https://adoptium.net/")
      }

      onStageChange("Extracting Java")
      try {
        await ipcRenderer.invoke("extractFile", {
          input: path.join(runtimePath, jreDownloadName),
          output: path.join(runtimePath, `jre`)
        });
      } catch (e) {
        throw throwCustomError("Failed to extract java", "We've not been able to extract Java... We can't recover from this.")
      }
      
      // Assuming this worked
      // Does the jre/jdk-* folder exist?
      onStageChange("Moving Java to the correct location")
      const jreFolder = fs.readdirSync(path.join(runtimePath, "jre")).find(e => e.startsWith("jdk-"));
      if (jreFolder == null) {
        throw throwCustomError("Failed to find java folder", "We've not been able to find the Java folder. We can't recover from this.")
      }
      
      // Move all of the folders contents to the runtime folder
      // Then delete the jre folder
      const jrePath = path.join(runtimePath, "jre", jreFolder);
      const jreFiles = fs.readdirSync(jrePath);
      jreFiles.forEach(e => {
        fs.renameSync(path.join(jrePath, e), path.join(runtimePath, e));
      });

      onStageChange("Cleaning up")
      try {
        // It's not fatal if this fails
        fs.rmdirSync(jrePath);
        fs.rmdirSync(path.join(runtimePath, 'jre'))
        fs.rmSync(path.join(runtimePath, jreDownloadName));
      } catch (e) {
        // Ignore
      }

      onStageChange("Finishing up")
      // Touch a file to note down what java version we have
      try {
        fs.writeFileSync(path.join(runtimePath, ".java-version"), javaVersion);
      } catch (e) {
        throw throwCustomError("Failed to write java version file", "We've not been able to write the java version file. We can't recover from this.")
      }

      onStageChange("Checking Java works")
      // Ensure the java version is executable and works
      const jreExecPath = jreLocation(appPath);
      if (!fs.existsSync(jreExecPath)) {
        throw throwCustomError("Failed to find java executable", "We've not been able to find the java executable. We can't recover from this.")
      }
      
      const result = execSync(`"${jreExecPath}" -version`);
      const resultString = result.toString();
      
      if (!resultString.includes("openjdk")) {
        throw throwCustomError("Failed to run java executable", "We've not been able to run the java executable. We can't recover from this.")
      }

      onStageChange("Starting subprocess")
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
              "-jar",
              `${resourcesPath}/${jarName}`,
              ...jvmArgs
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
        });
      } catch (e) {
        throw new CustomError("Failed to start subprocess after 3 attempts", "We've not been able to start the subprocess. We can't recover from this.")
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
        fs.writeFileSync(path.join(getAppHome(), "bin", ".first-launch"), "");
      }
    }
  },
  
  setupApp(vm) {
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
function retryAttempt<T>(fn: () => Promise<T>, attempts = 3): Promise<T> {
  return new Promise((resolve, reject) => {
    let attemptsLeft = attempts;
    
    const attempt = () => {
      fn()
        .then((res) => resolve(res))
        .catch((e) => {
          if (--attemptsLeft === 0) {
            reject(e);
          } else {
            setTimeout(() => attempt(), 1000);
          }
        });
    };
    
    attempt();
  });
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
