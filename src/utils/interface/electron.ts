// @ts-ignore no typescript package available
import VueNativeSock from 'vue-native-websocket';
import { clipboard, ipcRenderer } from 'electron';
import ElectronOverwolfInterface from './electron-overwolf-interface';
import fs from 'fs';
import path from 'path';
import store from '@/modules/store';
import { getAPIRequest } from '@/modules/modpacks/actions';
import { ModPack } from '@/modules/modpacks/types';
import router from '@/router';
import { logVerbose } from '@/utils';
import Vue from 'vue';
import axios from 'axios';
import EventEmitter from 'events';
import http from 'http';

declare const __static: string;

const contents = fs.existsSync(path.join(__static, 'version.json'))
  ? fs.readFileSync(path.join(__static, 'version.json'), 'utf-8')
  : null;
const jsonContent = contents ? JSON.parse(contents) : null;

class MiniWebServer extends EventEmitter {
  server: http.Server | null = null;
  timeoutRef: NodeJS.Timeout | null = null;

  open() {
    if (this.server == null) {
      this.server = http.createServer((req: any, res: any) => {
        let body = '';
        req.on('data', (chunk: any) => {
          body += chunk;
        });

        req.on('end', () => {
          const jsonResponse = JSON.parse(body);
          if (jsonResponse == null) {
            console.log('Failed to parse json response');
          }

          const { token, 'app-auth': appAuth } = jsonResponse;
          if (token == null || appAuth == null) {
            console.log('Failed to parse token or appAuth');
            return;
          }

          this.emit('response', jsonResponse);
          res.write('success');
          res.end();
          this.close();
        });
      });
      this.server.listen(7755, () => {
        console.log('MiniWebServer listening on 7755');
        this.emit('open');
      });
    } else {
      this.emit('open', false);
    }
  }

  closeAfterFive() {
    if (this.timeoutRef != null) {
      clearTimeout(this.timeoutRef);
    }

    this.timeoutRef = setTimeout(() => {
      this.emit('timeout');
      this.close();
    }, 1000 * 60 * 5);
  }

  close() {
    this.server?.close(() => {
      this.server = null;
      if (this.timeoutRef != null) {
        clearTimeout(this.timeoutRef);
      }
    });

    this.emit('close');
  }
}

const miniWebServer: MiniWebServer = new MiniWebServer();

const Electron: ElectronOverwolfInterface = {
  config: {
    appVersion: jsonContent?.jarVersion ?? 'Missing Version File',
    webVersion: jsonContent?.webVersion ?? 'Missing Version File',
    dateCompiled: jsonContent?.timestampBuilt ?? 'Missing Version File',
    javaLicenses: jsonContent?.javaLicense ?? {},
  },

  // Tools
  utils: {
    openUrl(url: string) {
      ipcRenderer.send('openLink', url);
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
    openLogin(cb: (data: { token: string; 'app-auth': string }) => void) {
      // TODO: see if we can re-implement this with the ftb:// protocol
      Electron.utils.openUrl(`https://minetogether.io/api/login?redirect=http://localhost:7755`);

      miniWebServer.open();
      miniWebServer.on('response', (data: { token: string; 'app-auth': string }) => {
        cb(data);
      });

      miniWebServer.on('open', () => {
        miniWebServer.closeAfterFive();
      });

      miniWebServer.on('close', () => {
        console.log('Web server closed');
      });
    },

    logoutFromMinetogether() {
      ipcRenderer.send('logout');
    },

    // Obviously do nothing
    changeExitOverwolfSetting() {},
    updateSettings(msg) {
      ipcRenderer.send('updateSettings', msg);
    },

    setUser(payload) {
      ipcRenderer.send('user', payload.user);
    },

    sendSession(payload) {
      ipcRenderer.send('session', payload);
    },

    onAppReady() {
      ipcRenderer.send('appReady');
    },

    uploadClientLogs() {},
    yeetLauncher() {},
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
  },

  // IO
  io: {
    selectFolderDialog(startPath, cb) {
      ipcRenderer.send('selectFolder', startPath);
      ipcRenderer.on('setInstanceFolder', (_, dir) => cb(dir));
    },
  },

  // Websockets
  websocket: {
    // Empty shim (this doesn't happen on overwolf)
    notifyWebhookReceived(message: string) {
      ipcRenderer.send('websocketReceived', message);
    },
  },

  setupApp(vm) {
    axios
      .get(`https://minetogether.io/api/adPool`)
      .then(res => {
        try {
          const id = parseInt(res.data, 10);
          if (id !== -1) {
            (window as any).adPoolID = null;
          } else {
            (window as any).adPoolID = id;
          }
        } catch (err) {
          (window as any).adPoolID = null;
        }
      })
      .catch(() => {
        (window as any).adPoolID = null;
      });

    ipcRenderer.send('sendMeSecret');
    ipcRenderer.on('hereIsSecret', (event, data) => {
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
        store.commit('STORE_WS', data);
        Vue.use(VueNativeSock, 'ws://localhost:' + data.port, { store, format: 'json', reconnection: true });
      }
    });
    ipcRenderer.send('gimmeAuthData');
    ipcRenderer.on('hereAuthData', (event, data) => {
      store.commit('auth/storeAuthDetails', data, { root: true });
    });
    ipcRenderer.on('setFriendsWindow', (event, data) => {
      store.dispatch('auth/setWindow', data, { root: true });
    });
    ipcRenderer.on('setSessionString', (event, data) => {
      const settings = store.state.settings?.settings;
      if (settings !== undefined) {
        settings.sessionString = data;
      }
      store.dispatch('settings/saveSettings', settings, { root: true });
    });
    ipcRenderer.on('getNewSession', (event, data) => {
      store.dispatch('auth/getNewSession', data, { root: true });
    });
    ipcRenderer.on('setSessionID', (event, data) => {
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
    ipcRenderer.on('openModpack', (event, data) => {
      const { name, id } = data;
      getAPIRequest(store.state, `modpack/search/8?term=${name}`)
        .then(response => response.json())
        .then(async data => {
          if (data.status === 'error') {
            return;
          }
          const packIDs = data.packs;
          if (packIDs == null) {
            return;
          }
          if (packIDs.length === 0) {
            return;
          }
          for (let i = 0; i < packIDs.length; i++) {
            const packID = packIDs[i];
            const pack: ModPack = await store.dispatch('modpacks/fetchModpack', packID, { root: true });
            if (pack !== undefined) {
              const foundVersion = pack.versions.find(v => v.mtgID === id);
              if (foundVersion !== undefined) {
                router.push({ name: 'modpackpage', query: { modpackid: packID } });
                return;
              }
            }
          }
        })
        .catch(err => {
          console.error(err);
        });
    });
    ipcRenderer.on('parseProtocolURL', (event, data) => {
      let protocolURL = data;
      if (protocolURL === undefined) {
        return;
      }
      protocolURL = protocolURL.substring(6, protocolURL.length);
      const parts = protocolURL.split('/');
      const command = parts[0];
      const args = parts.slice(1, parts.length);
      if (command === 'modpack') {
        if (args.length === 0) {
          return;
        }
        logVerbose(store.state, 'Received modpack protocol message', args);
        const modpackID = args[0];
        if (args.length === 1) {
          // Navigate to page for modpack
          logVerbose(store.state, 'Navigating to page for modpack', modpackID);
          router.push({ name: 'modpackpage', query: { modpackid: modpackID } });
        } else if (args.length === 2) {
          if (args[1] === 'install') {
            // Popup install for modpack
            logVerbose(store.state, 'Popping up install for modpack', modpackID);
            router.push({ name: 'modpackpage', query: { modpackid: modpackID, showInstall: 'true' } });
          }
        } else if (args.length === 3) {
          if (args[2] === 'install') {
            // Popup install for modpack with version default selected
            router.push({
              name: 'modpackpage',
              query: { modpackid: modpackID, showInstall: 'true', version: args[1] },
            });
          }
        }
      } else if (command === 'instance') {
        if (args.length === 0) {
          return;
        }
        const instanceID = args[0];
        if (args.length === 1) {
          // Open instance page
          router.push({ name: 'instancepage', query: { uuid: instanceID } });
        } else if (args.length === 2) {
          // Start instance
          router.push({ name: 'instancepage', query: { uuid: instanceID, shouldPlay: 'true' } });
        }
      } else if (command === 'server') {
        if (args.length === 0) {
          return;
        }
        const serverID = args[0];
        router.push({ name: 'server', query: { serverid: serverID } });
      }
    });
    ipcRenderer.on('sendWebsocket', (event, data) => {
      console.log('Request received to send ', data);
      const messageID = Math.round(Math.random() * 1000);
      data.requestId = messageID;
      data.secret = store.state.wsSecret;
      vm.$socket.sendObj(data);
    });
  },
};

export default Electron;
