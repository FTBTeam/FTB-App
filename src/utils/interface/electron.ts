import { ipcRenderer, clipboard } from 'electron';
import ElectronOverwolfInterface from './electron-overwolf-interface';
import fs from 'fs';
import path from 'path';

declare const __static: string;
const contents = fs.readFileSync(path.join(__static, 'version.json'), 'utf-8');
const jsonContent = JSON.parse(contents);

const Electron: ElectronOverwolfInterface = {
  config: {
    apiURL:
      process.env.NODE_ENV === 'production' ? `https://api.modpacks.ch` : `https://modpack-api-production.ch.tools`,
    appVersion: jsonContent.jarVersion ?? 'Missing Version File',
    webVersion: jsonContent.webVersion ?? 'Missing Version File',
    dateCompiled: jsonContent.timestampBuilt ?? 'Missing Version File',
    javaLicenses: jsonContent.javaLicense ?? {},
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
    // we don't need this on electron because it's not silly
    handleDrag() {},
    setupTitleBar() {},
  },
};

export default Electron;
