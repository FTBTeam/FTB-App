/**
 * This isn't my final form, I should be more unified and less hacky on some of
 * the overwolf methods as we're doing a lot of dropping of passed params on specific
 * methods. Not super ideal. Working for now
 */
export interface Util {
  openUrl: (e: string) => void;
}

export interface Actions {
  openModpack: (payload: { name: string; id: string }) => void;
  openFriends: () => void;
  openLogin: (cb: (data: any) => void) => void;

  // Res only used on overwolf
  setUser: (payload: any) => void;
  updateSettings: (info: any) => void;
  changeExitOverwolfSetting: (value: boolean) => void;
  logoutFromMinetogether: () => void;
}

export interface CB {
  copy: (e: string) => void;
  paste: () => string;
}

export interface Frame {
  close: (windowId: any, onClose: () => void) => void;
  min: (windowId: any) => void;
  max: (windowId: any) => void;

  expandWindow: () => void;
  collapseWindow: () => void;

  // Overwolf specific
  handleDrag: (event: any, windowId: any) => void;
  setupTitleBar: (cb: (windowId: any) => void) => void;
}

export interface Config {
  apiURL: string;
  appVersion: string;
  webVersion: string;
  dateCompiled: string;
  javaLicenses: object;
}

export interface InputOutput {
  selectFolderDialog: (startPath: string, cb: (selectedFile: string) => void) => void;
}

export interface Websocket {
  notifyWebhookReceived: (message: string) => void;
}

export default interface ElectronOverwolfInterface {
  utils: Util;
  actions: Actions;
  cb: CB;
  frame: Frame;
  config: Config;
  io: InputOutput;
  websocket: Websocket;
}
