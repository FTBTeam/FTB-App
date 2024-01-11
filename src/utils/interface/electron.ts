// @ts-ignore no typescript package available
import VueNativeSock from 'vue-native-websocket';
import {clipboard, ipcRenderer} from 'electron';
import ElectronOverwolfInterface from './electron-overwolf-interface';
import fs from 'fs';
import path from 'path';
import store from '@/modules/store';
import Vue from 'vue';
import EventEmitter from 'events';
import http from 'http';
import os from 'os';
import {handleAction} from '@/core/protocol/protocolActions';
import platform from '@/utils/interface/electron-overwolf';
import {emitter} from '@/utils';
import {AuthenticationCredentialsPayload} from '@/core/@types/authentication.types';
import log from 'electron-log';
import {createLogger} from '@/core/logger';
import {computeArch, computeOs, jreLocation} from '@/utils/interface/electron-helpers';
import {execSync} from 'child_process';

function getAppHome() {
  if (os.platform() === "darwin") {
    return path.join(os.homedir(), 'Library', 'Application Support', '.ftba');
  } else {
    return path.join(os.homedir(), '.ftba');
  }
}

const eLogger = createLogger("platform/electron.ts");
log.transports.file.resolvePath = (vars, message) => path.join(getAppHome(), 'logs', 'ftb-app-frontend.log');

Object.assign(console, log.functions);

declare const __static: string;

const contents = fs.existsSync(path.join(__static, 'version.json'))
  ? fs.readFileSync(path.join(__static, 'version.json'), 'utf-8')
  : null;
const jsonContent = contents ? JSON.parse(contents) : null;

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
    publicVersion: jsonContent?.publicVersion ?? 'Missing version file',
    appVersion: jsonContent?.jarVersion ?? 'Missing Version File',
    webVersion: jsonContent?.webVersion ?? 'Missing Version File',
    dateCompiled: jsonContent?.timestampBuilt ?? 'Missing Version File',
    javaLicenses: jsonContent?.javaLicense ?? {},
    branch: jsonContent.branch ?? 'Release'
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

    logoutFromMinetogether() {
      eLogger.debug("Logging out from minetogether")
      ipcRenderer.send('logout');
    },

    // Obviously do nothing
    changeExitOverwolfSetting() {},
    updateSettings(msg) {
      eLogger.debug("Updating settings", msg)
      ipcRenderer.send('updateSettings', msg);
    },

    setUser(payload) {
      eLogger.debug("Setting MT user")
      ipcRenderer.send('user', payload.user);
    },

    sendSession(payload) {
      eLogger.debug("Sending session (MT)")
      ipcRenderer.send('session', payload);
    },

    onAppReady() {
      eLogger.debug("Interface has been told the app is ready")
      ipcRenderer.send('appReady');
    },

    uploadClientLogs() {},
    yeetLauncher() {},

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
    handleDrag() {},
    setupTitleBar() {},
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

  // Websockets
  websocket: {
    // Empty shim (this doesn't happen on overwolf)
    notifyWebhookReceived(message: string) {
      ipcRenderer.send('websocketReceived', message);
    },
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
    async installApp(onStageChange: (stage: string) => void, onUpdate: (data: any) => void) {
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

      // Attempt to get the java
      const adopiumRequest = await fetch(`https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=${ourArch}&image_type=jre&os=${ourOs}`);
      const adopiumRes = await adopiumRequest.json();

      const binary = adopiumRes.find((e: any) => e.binary)?.binary
      const link = binary.package.link;

      const appPath = getAppHome();
      const runtimePath = `${appPath}/runtime`;

      // TODO: Validate this works
      if (!fs.existsSync(runtimePath)) {
        fs.mkdirSync(runtimePath, {
          recursive: true
        });
      }

      const res = await ipcRenderer.invoke("downloadFile", {
        url: link,
        path: `${runtimePath}/jre.tar.gz`
      });

      console.log(adopiumRes)
      console.log(res);
      
      if (!res) {
        throw new Error("Failed to download java");
      }
      
      // Extract the tar
      const extractResult = await ipcRenderer.invoke("extractFile", {
        input: `${runtimePath}/jre.tar.gz`,
        output: `${runtimePath}/jre`
      });
      
      console.log(extractResult);
      // Assuming this worked
      // Does the jre/jdk-* folder exist?
      const jreFolder = fs.readdirSync(`${runtimePath}/jre`).find(e => e.startsWith("jdk-"));
      if (jreFolder == null) {
        throw new Error("Failed to find jre folder");
      }
      
      // Move all of the folders contents to the runtime folder
      // Then delete the jre folder
      const jrePath = `${runtimePath}/jre/${jreFolder}`;
      const jreFiles = fs.readdirSync(jrePath);
      jreFiles.forEach(e => {
        fs.renameSync(`${jrePath}/${e}`, `${runtimePath}/${e}`);
      });
      
      fs.rmdirSync(jrePath);
      fs.rmdirSync(`${runtimePath}/jre`)
      fs.rmSync(`${runtimePath}/jre.tar.gz`)
      
      // Touch a file to note down what java version we have
      fs.writeFileSync(`${runtimePath}/.java-version`, jreFolder);
      
      // Ensure the java version is executable and works
      const jreExecPath = jreLocation(appPath);
      if (!fs.existsSync(jreExecPath)) {
        throw new Error("Failed to find java executable");
      }
      
      // TODO: Actually validate this
      const result = execSync(`"${jreExecPath}" -version`);
      console.log(result.toString());
      
      // Assuming this worked, we should tell the frontend to continue
    },
    async startSubprocess() {
      // TODO: Get the args from the meta file
      
      // Get the runtime
      const appPath = getAppHome();
      const runtimePath = `${appPath}/runtime`;
      
      if (!fs.existsSync(`${runtimePath}/.java-version`)) {
        throw new Error("Missing java version");
      }
      
      // TODO: Check if this needs updating I guess
      
      // Start the subprocess
      const jreExecPath = jreLocation(appPath);
      if (!fs.existsSync(jreExecPath)) {
        throw new Error("Failed to find java executable");
      }
      
      await ipcRenderer.invoke("startSubprocess", {
        javaPath: jreExecPath,
        args: [
          "-jar",
          `${appPath}/bin/FTBApp.jar`,
          "--dev"
        ]
      });
      
      // Finally, if the above worked, tell the app to connect to the WS protocol
    }
  },
  
  setupApp(vm) {
    eLogger.debug("Setting up the app from the interface on electron")
    ipcRenderer.send('sendMeSecret');
    ipcRenderer.on('hereIsSecret', (event, data) => {
      eLogger.debug("Received secret from main process", data)
      if (data.port === 13377 && !data.isDevMode) {
        Vue.use(VueNativeSock, 'ws://localhost:' + data.port, {
          format: 'json',
          reconnection: true,
          connectManually: true,
        });
        vm.$connect();
        vm.$socket.onmessage = (msgData: MessageEvent) => {
          const wsInfo = JSON.parse(msgData.data);
          store.commit('STORE_WS', wsInfo);
          vm.$disconnect();
          const index = Vue._installedPlugins.indexOf(VueNativeSock);
          if (index > -1) {
            Vue._installedPlugins.splice(index, 1);
          }
          Vue.use(VueNativeSock, 'ws://localhost:' + wsInfo.port, { store, format: 'json', reconnection: true });
          ipcRenderer.send('updateSecret', wsInfo);
        };
      } else {
        eLogger.debug("Setting up websocket connection on port", data.port)
        store.commit('STORE_WS', data);
        Vue.use(VueNativeSock, 'ws://localhost:' + data.port, { store, format: 'json', reconnection: true });
      }
    });
    
    eLogger.debug("Requesting auth data")
    ipcRenderer.send('gimmeAuthData');
    ipcRenderer.on('hereAuthData', (event, data) => {
      eLogger.debug("Received auth data from main process", data)
      store.commit('auth/storeAuthDetails', data, { root: true });
    });
    ipcRenderer.on('setFriendsWindow', (event, data) => {
      store.dispatch('auth/setWindow', data, { root: true });
    });
    ipcRenderer.on('auth-window-closed', (event, data) => {
      miniServers.forEach((server) => {
        server.close().then(() => {
          eLogger.debug("Closing mini server")
        });
      });

      miniServers = [];
    });
    ipcRenderer.on('setSessionString', (event, data) => {
      eLogger.debug("Received session string from main process")
      const settings = store.state.settings?.settings;
      if (settings !== undefined) {
        settings.sessionString = data;
      }
      store.dispatch('settings/saveSettings', settings, { root: true });
    });
    ipcRenderer.on('getNewSession', (event, data) => {
      eLogger.debug("Requesting new session from main process")
      store.dispatch('auth/getNewSession', data, { root: true });
    });
    ipcRenderer.on('setSessionID', (event, data) => {
      eLogger.debug("Setting session ID from main process")
      store.dispatch('auth/setSessionID', data, { root: true });
    });
    ipcRenderer.on('blockFriend', (event, data) => {
      const settings = store.state.settings?.settings;
      if (settings !== undefined && settings.blockedUsers === undefined) {
        settings.blockedUsers = [];
      }
      if (typeof settings?.blockedUsers !== 'string') {
        settings?.blockedUsers.push(data);
      }
      store.dispatch('settings/saveSettings', settings, { root: true });
    });
    // // TODO: (M#01) Yeet me
    // ipcRenderer.on('openModpack', (event, data) => {
    //   const { name, id } = data;
    //   getAPIRequest(store.state, `modpack/search/8?term=${name}`)
    //     .then((response) => response.json())
    //     .then(async (data) => {
    //       if (data.status === 'error') {
    //         return;
    //       }
    //       const packIDs = data.packs;
    //       if (packIDs == null) {
    //         return;
    //       }
    //       if (packIDs.length === 0) {
    //         return;
    //       }
    //       for (let i = 0; i < packIDs.length; i++) {
    //         const packID = packIDs[i];
    //         const pack: ModPack = await store.dispatch('modpacks/fetchModpack', packID, { root: true });
    //         if (pack !== undefined) {
    //           const foundVersion = pack.versions.find((v) => v.mtgID === id);
    //           if (foundVersion !== undefined) {
    //             router.push({ name: 'modpackpage', query: { modpackid: packID } });
    //             return;
    //           }
    //         }
    //       }
    //     })
    //     .catch((err) => {
    //       console.error(err);
    //     });
    // });
    
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
      // } else if (command === 'instance') {
      //   if (args.length === 0) {
      //     return;
      //   }
      //   const instanceID = args[0];
      //   if (args.length === 1) {
      //     // Open instance page
      //     router.push({ name: 'instancepage', query: { uuid: instanceID } });
      //   } else if (args.length === 2) {
      //     // Start instance
      //     router.push({ name: 'instancepage', query: { uuid: instanceID, shouldPlay: 'true' } });
      //   }
      // } else if (command === 'server') {
      //   if (args.length === 0) {
      //     return;
      //   }
      //   const serverID = args[0];
      //   router.push({ name: 'server', query: { serverid: serverID } });
      // }
    });
  },
};

export default Electron;
