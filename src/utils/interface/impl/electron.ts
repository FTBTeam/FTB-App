// @ts-ignore no typescript package available
import ElectronOverwolfInterface from '../electron-overwolf-interface.ts';
import { handleAction } from '@/core/protocol/protocolActions.ts';
import { parseArgs } from '@/utils/interface/electron-helpers.ts';
import { getAppHome, jreHome } from '@/utils/nuturalHelpers.ts';
import { retrying } from '@/utils/helpers/asyncHelpers.ts';

const { fs, os, path } = window.nodeUtils;

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

const appHome = getAppHome(os.platform(), os.homedir(), path.join);

// const electronFrontendLogFile = path.join(appHome, 'logs', 'ftb-app-frontend.log');
// if (fs.exists(electronFrontendLogFile)) {
//   try {
//     fs.writeFile(electronFrontendLogFile, '')
//   } catch (e) {
//     console.error("Failed to clear electron frontend log file", e)
//   }
// }

const metaData = window.nodeUtils.app.getMetaData();
console.info("Metadata", metaData)

const licenses = window.nodeUtils.app.getLicenses();

const licensesData: any = licenses.frontend;
const javaLicensesData: any = licenses.java;

const Electron: ElectronOverwolfInterface = {
  isElectron: true,
  isOverwolf: false,
  
  config: {
    version: metaData.appVersion ?? 'Missing Version File',
    commit: metaData.commit ?? 'Missing Version File',
    dateCompiled: metaData.released.toString() ?? new Date().getTime(),
    branch: metaData.branch ?? 'release'
  },

  // Tools
  utils: {
    openUrl(url: string) {
      window.ipcRenderer.send('action/open-link', url);
    },

    async getOsArch() {
      return await window.ipcRenderer.invoke("os/arch");
    },
    
    async getOsType() {
      return await window.ipcRenderer.invoke("os/platform");
    },

    async getPlatformVersion() {
      return process.versions.electron;
    },
    
    crypto: {
      randomUUID(): string {
        // This is a web function, not a node function
        return crypto.randomUUID();
      }
    },
    
    openDevTools() {
      window.ipcRenderer.send('action/open-dev-tools');
    },
  },

  // Actions
  actions: {
    // Obviously do nothing
    changeExitOverwolfSetting() {},    

    onAppReady() {
      console.debug("Interface has been told the app is ready")
      window.ipcRenderer.send('event/app-ready');
    },

    uploadClientLogs() {},

    restartApp() {
      console.debug("Restarting app")
      // Restart the electron app
      window.ipcRenderer.send('action/reload-main-window');
    }
  },

  // Clipboard
  cb: {
    copy(e: string) {
      window.ipcRenderer.send("action/write-to-clipboard", e);
    },
  },

  // Frame / Chrome / Window / What ever you want to call it
  frame: {
    close() {
      window.ipcRenderer.send('action/control-window', { action: 'close' });
    },
    min() {
      window.ipcRenderer.send('action/control-window', { action: 'minimize' });
    },
    max() {
      window.ipcRenderer.send('action/control-window', { action: 'maximize' });
    },

    quit() {
      window.ipcRenderer.send('action/quit-app');
    },

    // we don't need this on electron because it's not silly
    async getWindowId() {
      return ""
    },
    handleDrag() {},
    setSystemWindowStyle(enabled) {
      window.ipcRenderer.invoke('setSystemWindowStyle', enabled);
    }
  },

  // IO
  io: {
    selectFolderDialog(startPath, cb) {
      window.ipcRenderer
        .invoke('action/select-folder', startPath)
        .then((dir) => cb(dir))
        .catch((e) => {
          console.warn("Failed to select folder from the system", e)
          cb(null)
        });
    },

    selectFileDialog(cb) {
      window.ipcRenderer
        .invoke('action/select-file')
        .then((dir) => {
          cb(dir);
        })
        .catch((e) => {
          console.warn("Failed to select file from the system", e)
          cb(null)
        });
    },
    
    openFinder(path: string): Promise<boolean> {
      return window.ipcRenderer.invoke('action/open-finder', path);
    },
    
    pathJoin(...paths: string[]) {
      return path.join(...paths);
    },
    
    appHome() {
      return appHome;
    }
  },
  
  app: {
    async appSettings() {
      const settingsPath = path.join(appHome, "bin", "settings.json");
      if (!fs.exists(settingsPath)) {
        return null
      }

      return JSON.parse(fs.readFile(settingsPath, 'utf-8'));
    },
    async appData(): Promise<string> {
      return path.join(appHome, 'bin');
    },
    async startSubprocess() {
      // Get the runtime
      const appPath = appHome;
      const runtimePath = `${appPath}/runtime`;
      
      if (!fs.exists(`${runtimePath}/.java-version`)) {
        throw new CustomError("Failed to find java version file", "We've not been able to find the java version file. We can't recover from this.");
      }
      
      const javaVersion = fs.readFile(`${runtimePath}/.java-version`, 'utf-8');
      if (!javaVersion) {
        throw new CustomError("Failed to read java version file", "We've not been able to read the java version file. We can't recover from this.");
      }
      
      if (javaVersion !== metaData.runtime.version) {
        // Looks like we need to try and update the java version
        return false;
      }
      
      // Start the subprocess
      const jreExecPath = jreHome(appPath, path.join, os.platform() === "win32", os.platform() === "darwin");
      if (!fs.exists(jreExecPath)) {
        throw new Error("Failed to find java executable");
      }
      
      const envVars = parseArgs(metaData.runtime.env);
      const jvmArgs = parseArgs(metaData.runtime.jvmArgs);
      
      const jarName = metaData.runtime.jar;
      
      try {
        return await retrying(async () => {
          const {port, secret} = await window.ipcRenderer.invoke("startSubprocess", {
            javaPath: jreExecPath,
            args: [
              ...jvmArgs,
              "-jar",
              `${window.nodeUtils.path.resourcesPath}/${jarName}`,
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
    async appChannel() {
      return await window.ipcRenderer.invoke("action/app/get-channel");
    },
    async changeAppChannel(channel: string) {
      return await window.ipcRenderer.invoke("action/app/change-channel", channel);
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
        return await window.ipcRenderer.invoke("ow/cpm/is-required");
      },
      async openWindow(arg = "purposes") {
        // Required for OW Ads
        await window.ipcRenderer.invoke("ow/cpm/open-window", arg);
      },
      async isFirstLaunch() {
        return !fs.exists(path.join(appHome, "bin", ".first-launch"));
      },
      async setFirstLaunched() {
        // Create parents
        if (!fs.exists(path.join(appHome, "bin"))) {
          fs.mkdir(path.join(appHome, "bin"), {
            recursive: true
          });
        }
        
        fs.writeFile(path.join(appHome, "bin", ".first-launch"), "");
      }
    }
  },
  
  setupApp() {
    console.debug("Setting up the app from the interface on electron")
    
    window.ipcRenderer.on('parseProtocolURL', (_, data) => {
      handleAction(data);
    });
  },
};

class CustomError extends Error {
  customMessage: string;

  constructor(message: string, customMessage: string) {
    super(message);
    this.name = "CustomError";
    this.customMessage = customMessage;
  }
}

export default Electron;