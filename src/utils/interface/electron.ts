import { ipcRenderer, clipboard } from 'electron';
import ElectronOverwolfInterface from './electron-overwolf-interface';
import fs from 'fs';
import path from 'path';

declare const __static: string;

const contents = fs.existsSync(path.join(__static, 'version.json'))
  ? fs.readFileSync(path.join(__static, 'version.json'), 'utf-8')
  : null;
const jsonContent = contents ? JSON.parse(contents) : null;

const Electron: ElectronOverwolfInterface = {
  config: {
    apiURL:
      process.env.NODE_ENV === 'production' ? `https://api.modpacks.ch` : `https://modpack-api-production.ch.tools`,
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
    openLogin() {
      ipcRenderer.send('openOauthWindow');
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
};

export default Electron;
