import ElectronOverwolfInterface from './electron-overwolf-interface';

declare global {
  var overwolf: any;
}

const versionData = overwolf.windows.getMainWindow().getVersionData();

const getWindowState = (windowId: any) => {
  return new Promise((resolve, reject) => {
    //@ts-ignore
    overwolf.windows.getWindowState(windowId, res => {
      if (res && res.status === 'success') {
        resolve(res.window_state_ex);
      } else {
        reject(res);
      }
    });
  });
};

const Overwolf: ElectronOverwolfInterface = {
  config: {
    apiURL:
      process.env.NODE_ENV === 'production' ? `https://api.modpacks.ch` : `https://modpack-api-production.ch.tools`,
    appVersion: versionData.jarVersion ?? 'Missing Version File',
    webVersion: versionData.webVersion ?? 'Missing Version File',
    dateCompiled: versionData.timestampBuilt ?? 'Missing Version File',
    javaLicenses: versionData.javaLicense ?? {},
  },

  // Tools
  utils: {
    openUrl(url: string) {
      overwolf.openUrlInDefaultBrowser(url);
    },
  },

  // Actions
  actions: {
    openModpack(payload) {
      overwolf.utils.openUrlInDefaultBrowser(`ftb://modpack/${payload.id}`);
    },

    openFriends() {
      overwolf.windows.obtainDeclaredWindow('chat', (result: any) => {
        if (result.success && !result.window.isVisible) {
          overwolf.windows.restore(result.window.id);
        }
      });
    },

    async openLogin(cb: (data: any) => void) {
      await overwolf.windows.getMainWindow().openWebserver(cb);
      overwolf.utils.openUrlInDefaultBrowser(`https://minetogether.io/api/login?redirect=http://localhost:7755`);
    },

    changeExitOverwolfSetting(value: boolean) {
      overwolf.settings.setExtensionSettings({ exit_overwolf_on_exit: value }, (data: any) => {
        console.log(data);
      });
    },

    logoutFromMinetogether() {
      overwolf.windows.obtainDeclaredWindow('chat', (result: any) => {
        if (result.success && result.window.isVisible) {
          overwolf.windows.close(result.window.id);
        }
      });
    },

    setUser(payload) {
      overwolf.windows.getMainWindow().setAuthData(payload);
    },

    // Nothing done for reasons
    updateSettings() {},
  },

  // Clipboard
  cb: {
    copy(e: string) {
      overwolf.utils.placeOnClipboard(e);
    },
    paste(): string {
      let str = '';

      overwolf.utils.getFromClipboard((clipboard: string) => {
        str = clipboard;
      });

      return str ?? '';
    },
  },

  // Frame / Chrome / Window / What ever you want to call it
  frame: {
    close(windowId: any, onClose: () => void) {
      // TODO: if exitOverwolf is enabled, ensure Overwolf exists
      overwolf.windows.close(windowId);

      // @ts-ignore we don't know what the window is.
      if (window.isChat === undefined || window.isChat === false) {
        onClose();
      }
    },
    min(windowId: any) {
      overwolf.windows.minimize(windowId);
      //@ts-ignore
      if (window.ad) {
        //@ts-ignore
        window.ad.removeAd();
      }
    },
    async max(windowId: any) {
      let maximised = await getWindowState(windowId);
      if (maximised === 'maximized') {
        overwolf.windows.restore(windowId);
        //@ts-ignore
        if (window.ad) {
          //@ts-ignore
          window.ad.refreshAd();
        }
      } else {
        overwolf.windows.maximize(windowId);
      }
    },
    // we don't need this on electron because it's not silly
    handleDrag(event: any, windowId: any) {
      if (event.target.classList.contains('title-action')) {
        return;
      }

      overwolf.windows.dragMove(windowId);
    },
    setupTitleBar(cb: (windowId: any) => void) {
      overwolf.windows.getCurrentWindow((result: any) => {
        if (result && result.status === 'success' && result.window && result.window.id) {
          cb(result.window.id);
        }
      });
    },
    expandWindow() {
      overwolf.windows.getCurrentWindow((result: any) => {
        if (result && result.status === 'success' && result.window && result.window.id) {
          //@ts-ignore
          overwolf.windows.changeSize(result.window.id, 800, result.window.height);
        }
      });
    },
    collapseWindow() {
      overwolf.windows.getCurrentWindow((result: any) => {
        if (result && result.status === 'success' && result.window && result.window.id) {
          //@ts-ignore
          overwolf.windows.changeSize(result.window.id, 300, result.window.height);
        }
      });
    },
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
  },

  // Websockets
  websocket: {
    // Empty shim (this doesn't happen on overwolf)
    notifyWebhookReceived(message: string) {},
  },
};

export default Overwolf;

// FIXME: Code I found not being used anywhere

// private readDirSync(path: string): Promise<Array<any>> {
//   return new Promise((resolve, reject) => {
//       //@ts-ignore
//       overwolf.io.dir(path, (result) => {
//           if(result.success === true && result.data){
//               resolve(result.data);
//           }
//       })
//   });
// }

// private readFileSync(path: string, options?: any): Promise<string> {
//   return new Promise((resolve, reject) => {
//       //@ts-ignore
//       overwolf.io.readTextFile(path, options, (result) => {
//           if(result.success === true){
//               resolve(result.content);
//           } else {
//               reject(result.error);
//           }
//       });
//   });
// }

// private existsSync(path: string): Promise<boolean> {
//   return new Promise((resolve, reject) => {
//       //@ts-ignore
//       overwolf.io.exist(path,(result) => {
//           if(result.success === true){
//               resolve(result.exist);
//           } else {
//               reject(result.error);
//           }
//       });
//   });
// }
