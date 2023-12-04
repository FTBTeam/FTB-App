import {AuthenticationCredentialsPayload} from '@/core/@types/authentication.types';

/**
 * This isn't my final form, I should be more unified and less hacky on some of
 * the overwolf methods as we're doing a lot of dropping of passed params on specific
 * methods. Not super ideal. Working for now
 */
export interface Util {
  /**
   * @deprecated don't use
   */
  getOsArch: () => string;
  getPlatformVersion: () => Promise<string>;
  openUrl: (e: string) => void;
  crypto: {
    randomUUID(): string;
  }
  openDevTools: () => void;
}

export interface Actions {
  openMsAuth(): void;
  emitAuthenticationUpdate(credentials?: AuthenticationCredentialsPayload): void;
  openModpack: (payload: { name: string; id: string }) => void;
  closeWebservers(): void;
  openFriends: () => void;
  openLogin: (cb: (data: { token: string; 'app-auth': string }) => void) => void;
  uploadClientLogs: () => void;

  // Res only used on overwolf
  setUser: (payload: any) => void;
  updateSettings: (info: any) => void;
  changeExitOverwolfSetting: (value: boolean) => void;
  logoutFromMinetogether: () => void;
  sendSession: (payload: any) => void;
  yeetLauncher: (windowId: any, cb: () => void) => void;
  onAppReady: () => void;

  restartApp(): void;
}

export interface CB {
  copy: (e: string) => void;
  paste: () => string;
}

export interface Frame {
  close: (windowId: any, onClose: () => void) => void;
  min: (windowId: any) => void;
  max: (windowId: any) => void;
  quit: () => void;

  expandWindow: () => void;
  collapseWindow: () => void;

  // Overwolf specific
  handleDrag: (event: any, windowId: any) => void;
  setupTitleBar: (cb: (windowId: any) => void) => void;
  setSystemWindowStyle(enabled: boolean): void;
}

export interface Config {
  publicVersion: string;
  appVersion: string;
  webVersion: string;
  dateCompiled: string;
  javaLicenses: object;
  branch: string
}

export interface InputOutput {
  selectFolderDialog: (startPath: string, cb: (selectedFile: string | null) => void) => void;
  selectFileDialog: (cb: (selectedFile: string | null) => void) => void;
  
  openFinder(path: string): Promise<boolean>;
  
  getLocalAppData: () => string;
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
  setupApp: (vm: any) => void;
}
