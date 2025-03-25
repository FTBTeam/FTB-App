import {JavaLicenses, JavascriptLicenses} from '@/core/types/external/licenses.types';

/**
 * This isn't my final form, I should be more unified and less hacky on some of
 * the overwolf methods as we're doing a lot of dropping of passed params on specific
 * methods. Not super ideal. Working for now
 */
export interface Util {
  getOsArch: () => Promise<string>;
  getOsType: () => Promise<string>;
  getPlatformVersion: () => Promise<string>;
  openUrl: (e: string) => void;
  crypto: {
    randomUUID(): string;
  }
  openDevTools: () => void;
}

export interface Actions {
  openModpack: (payload: { name: string; id: string }) => void;
  openFriends: () => void;
  uploadClientLogs: () => void;

  // Res only used on overwolf
  changeExitOverwolfSetting: (value: boolean) => void;
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
  getWindowId: () => Promise<string>;
  handleDrag: (event: any, windowId: any) => void;
  setSystemWindowStyle(enabled: boolean): void;
}

export interface Config {
  version: string;
  dateCompiled: string;
  commit: string;
  branch: string
}

export interface InputOutput {
  selectFolderDialog: (startPath: string, cb: (selectedFile: string | null) => void) => void;
  selectFileDialog: (cb: (selectedFile: string | null) => void) => void;
  
  openFinder(path: string): Promise<boolean>;
  pathJoin(...paths: string[]) : string;
  
  getLocalAppData: () => string;
  appHome: () => string;
}

export interface App {
  appData(): Promise<string>;
  appSettings(): Promise<any | null>;
  appRuntimes(): Promise<string>;
  runtimeAvailable(): Promise<boolean>;
  installApp(onStageChange: (stage: string) => void, onUpdate: (data: any) => void, isUpdate: boolean): Promise<void>;
  updateApp(onStageChange: (stage: string) => void, onUpdate: (data: any) => void): Promise<void>;
  startSubprocess(): Promise<{ 
    port: number;
    secret: string;
  } | false>;
  
  getLicenses(): {
    javascript: JavascriptLicenses | null,
    java: JavaLicenses | null,
  };
  
  appChannel(): Promise<string>;
  changeAppChannel(channel: string): Promise<void>;
  
  cpm: {
    required(): Promise<boolean>;
    openWindow(tab: 'purposes' | 'features' | 'vendors'): Promise<void>;
    isFirstLaunch: () => Promise<boolean>;
    setFirstLaunched: () => Promise<void>;
  }
}

export default interface ElectronOverwolfInterface {
  utils: Util;
  actions: Actions;
  cb: CB;
  frame: Frame;
  config: Config;
  io: InputOutput;
  app: App;
  setupApp: () => void;
}
