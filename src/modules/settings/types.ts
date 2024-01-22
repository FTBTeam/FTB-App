export interface Settings {
  width: number;
  height: number;
  memory: number;
  jvmargs: string;
  shellArgs: string;
  enableChat: boolean;
  enablePreview: boolean;
  threadLimit: number;
  speedLimit: number;
  cacheLife: number;
  instanceLocation: string;
  verbose: boolean;
  autoOpenChat: boolean | string;
  blockedUsers: string[] | string;
  sessionString?: string;
  showAdverts: boolean | string;
  exitOverwolf?: boolean;
  updateChannel: string;
  fullScreen: boolean;
  
  useSystemWindowStyle: boolean;
  
  // new ish
  proxyHost: string;
  proxyPort: number;
  proxyUser: string;
  proxyPassword: string;
  proxyType: 'http' | 'sock5' | 'none';
}

export interface Hardware {
  totalMemory: number;
  totalCores: number;
  availableMemory: number;
  mainScreen: Resolution;
  supportedResolutions: Resolution[];
}

export interface SettingsState {
  settings: Settings;
  error: boolean;
  hardware: Hardware;
}

export interface JavaVersion {
  name: string;
  path: string;
}

export interface Resolution {
  width: number;
  height: number;
}
