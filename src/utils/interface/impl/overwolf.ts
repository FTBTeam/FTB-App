import ElectronOverwolfInterface from '../electron-overwolf-interface.ts';
import {handleAction} from '@/core/protocol/protocolActions.ts';
import {createLogger, logger} from '@/core/logger.ts';

declare global {
  var overwolf: any;
}

const versionData = overwolf.windows.getMainWindow().getVersionData();

const owLogger = createLogger('platform/overwolf.ts');

/**
 * TODO: Remove, electron port issues
 */
export type MetaData = {
  appVersion: string;
  commit: string;
  branch: string;
  released: number;
  runtime: {
    version: string;
    jar: string;
    env: any[];
    jvmArgs: any[];
  };
}

function parseAndHandleURL(protocolURL: string) {
  handleAction(protocolURL);
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
}


const getWindowState = (windowId: any) => {
  return new Promise((resolve, reject) => {
    //@ts-ignore
    overwolf.windows.getWindowState(windowId, (res) => {
      if (res && res.status === 'success') {
        resolve(res.window_state_ex);
      } else {
        reject(res);
      }
    });
  });
};

const Overwolf: ElectronOverwolfInterface = {
  isOverwolf: true,
  isElectron: false,
  
  config: {
    version: versionData.appVersion ?? 'Missing Version File',
    commit: versionData.commit ?? 'Missing Version File',
    dateCompiled: versionData.released ?? 'Missing Version File',
    branch: versionData.branch ?? 'Release'
  },

  // Tools
  utils: {
    openUrl(url: string) {
      overwolf.utils.openUrlInDefaultBrowser(url);
    },

    async getOsArch() {
      return 'x64'
    },
    
    async getOsType() {
      return 'windows'
    },

    async getPlatformVersion() {
      return new Promise((resolve) => {
        overwolf.extensions.current.getManifest((app: any) => resolve(app.meta.version));
      });
    },
    
    crypto: {
      randomUUID(): string {
        return overwolf.windows.getMainWindow().funcs().randomUUID() as string;
      }
    },

    openDevTools() {
      // No way to do this on overwolf atm
    }
  },

  // Actions
  actions: {
    changeExitOverwolfSetting(value: boolean) {
      overwolf.settings.setExtensionSettings({ exit_overwolf_on_exit: value }, () => {});
    },
    
    uploadClientLogs() {
      overwolf.utils.uploadClientLogs(() => {
      });
    },

    // Nothing done for reasons
    onAppReady() {
      //@ts-ignore
      let launchedProtoURL = overwolf.windows.getMainWindow().getProtocolURL();
      if (launchedProtoURL !== undefined) {
        parseAndHandleURL(launchedProtoURL);
      }
    },
    
    restartApp() {
      owLogger.debug("Restarting app")
      overwolf.windows.getMainWindow().funcs().restartApp();
    }
  },

  // Clipboard
  cb: {
    copy(e: string) {
      overwolf.utils.placeOnClipboard(e);
    },
  },

  // Frame / Chrome / Window / What ever you want to call it
  frame: {
    close(windowId: any, onClose: () => void) {
      overwolf.windows.close(windowId);

      // @ts-ignore we don't know what the window is.
      if (window.isChat === undefined || window.isChat === false) {
        onClose();
      }
    },
    min(windowId: any) {
      overwolf.windows.minimize(windowId);
    },
    async max(windowId: any) {
      let maximised = await getWindowState(windowId);
      if (maximised === 'maximized') {
        overwolf.windows.restore(windowId);
      } else {
        overwolf.windows.maximize(windowId);
      }
    },
    async getWindowId() {
      return new Promise((resolve, reject) => {
        overwolf.windows.getCurrentWindow((result: any) => {
          if (result && result.status === 'success' && result.window && result.window.id) {
            resolve(result.window.id as string);
          } else {
            reject(result);
          }
        });
      });
    },
    // we don't need this on electron because it's not silly
    handleDrag(event: any, windowId: any) {
      if (event.target.classList.contains('title-action')) {
        return;
      }

      overwolf.windows.dragMove(windowId);
    },
    quit() {
      overwolf.windows.getCurrentWindow((result: any) => {
        if (result.success) {
          //@ts-ignore
          overwolf.windows.close(result.window.id);
        }
      });
    },
    setSystemWindowStyle() {}
  },

  // IO
  io: {
    selectFolderDialog(startPath, cb) {
      overwolf.utils.openFolderPicker(startPath, (resp: any) => {
        if (resp && resp.status === 'success') {
          cb(resp.path);
        }
      });
    },

    selectFileDialog(filter, cb) {
      overwolf.utils.openFilePicker(filter === null ? '' : filter, (resp: any) => {
        if (resp && resp.status === 'success') {
          cb(resp.file);
        }
      });
    },
    
    openFinder(path: string): Promise<boolean> {
      return new Promise((resolve) => {
        overwolf.utils.openWindowsExplorer(path, (result: any) => {
          resolve(result.status === 'success');
        });
      });
    },
    
    pathJoin(...paths: string[]) {
      // Because we're on windows we need to use backslashes
      return paths.join('\\');
    },
    
    appHome() {
      return `${overwolf.io.paths.localAppData}\\.ftba`;
    }
  },

  app: {
    async appSettings() {
      // Don't use this on overwolf
      throw new Error("Don't use app.appSettings() on Overwolf")
    },
    async appData() {
      // Don't use this on overwolf
      throw new Error("Don't use app.appData() on Overwolf")
    },
    async changeAppChannel(_: string) {
      return;
    },
    async appChannel() {
      return "";
    },
    /**
     * Because the subprocess is already started in this instance. We're just going to askk 
     * the main window for the credentials so we can connect
     */
    async startSubprocess() {
      const mainWindow = overwolf.windows.getMainWindow();

      /**
       * It's possible it won't be ready yet, so we'll wait for it to be ready
       */
      logger.log("Attempting to get ws data")
      let attempts = 0;
      return new Promise((resolve, reject) => {
        const interval = setInterval(() => {
          const data = mainWindow.funcs().wsData()
          if (data) {
            clearInterval(interval)
            resolve({
              port: data.port,
              secret: data.secret
            });
          } else {
            attempts++;
            if (attempts > 30) {
              clearInterval(interval);
              reject("Unable to get ws data")
            }
          }
        }, 1_000) // 1 second
      })
    },
    getLicenses() {
      // Get main window
      const mainWindow = overwolf.windows.getMainWindow();
      return {
        javascript: mainWindow.getLicenseData(),
        java: mainWindow.getJavaLicenseData()
      }
    },
    cpm: {
      async required() {
        return false;
      },
      async openWindow() {
        // IGNORED
      },
      async isFirstLaunch() {
        return false;
      },
      async setFirstLaunched() {},
    }
  },

  setupApp() {
    owLogger.info('Setting up app for overwolf')
    // setup websockets and the actual window

    new Promise(resolve => {
      overwolf.utils.getMonitorsList(async (result: any) => {
        // Get the window scaling
        const scale = window.devicePixelRatio;

        // Work out the smallest monitor and use that as the max height we can work within
        if (!result.displays) {
          owLogger.debug("No displays found")
          return;
        }

        let maxHeight = Infinity;
        result.displays.forEach((display: any) => {
          const windowHeight = Math.floor(display.height / scale);
          maxHeight = Math.min(maxHeight, windowHeight);
        });

        owLogger.info("Max height found", maxHeight)
        
        resolve(maxHeight)
      });
    })
      .then(async (height: any) => {
        const manifestData: any = await new Promise(resolve => overwolf.extensions.current.getManifest((manifest: any) => resolve(manifest)));
        const indexWindow = manifestData.data.windows.index;
  
        owLogger.info("Index window", indexWindow)
        
        if (height < 880) {
          owLogger.info("Setting min size for index window")
          overwolf.windows.setMinSize("index", indexWindow.min_size.width, 700, (e: any) => owLogger.debug("Set min size error", e));
          if (!(window as any).ftbFlags) {
            (window as any).ftbFlags = {};
          }
  
          (window as any).ftbFlags.smallMonitor = true;
        }
      })
    
    //@ts-ignore
    overwolf.extensions.onAppLaunchTriggered.addListener(function (event) {
      if (event.origin === 'urlscheme') {
        let protocolURL = event.parameter;
        if (protocolURL === undefined) {
          return;
        }
        protocolURL = decodeURIComponent(protocolURL);
        parseAndHandleURL(protocolURL);
      }
    });

    addWindowListener().catch(owLogger.error);

    async function addWindowListener() {
      let ourWindowID = await new Promise((resolve) => {
        overwolf.windows.getCurrentWindow((e: any) => {
          if (e.success) {
            resolve(e.window.id);
          }
        });
      });

      overwolf.windows.onStateChanged.addListener((event: any) => {
        owLogger.info('Window state changed', event)
        const windowAd: any = (window as any).ads;
        if (!windowAd) {
          owLogger.warn("Unable to control ad as it's not set");
          return;
        }
        
        if (event.window_id === ourWindowID) {
          if (
            event.window_previous_state_ex === 'minimized' &&
            (event.window_state_ex === 'normal' || event.window_state_ex === 'maximized')
          ) {
            if (windowAd) {
              Object.values(windowAd.ads ?? {}).forEach((e: any) => e.refreshAd());
              owLogger.info('Refreshing owAd');
            }
          } else if (
            event.window_state_ex === 'minimized' &&
            (event.window_previous_state_ex === 'normal' || event.window_previous_state_ex === 'maximized')
          ) {
            if (windowAd) {
              Object.values(windowAd.ads ?? {}).forEach((e: any) => e.removeAd());
              owLogger.info('removing owAd');
            }
          }
        }
      });
    }
  },
};

export default Overwolf;
