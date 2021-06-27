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
  },
};

export default Overwolf;
