export interface Settings {
    width: number;
    height: number;
    memory: number;
    keepLauncherOpen: boolean;
    jvmargs: string;
    enableAnalytics: boolean;
    enableBeta: boolean;
    enablePreview: boolean;
    threadLimit: number;
    speedLimit: number;
    cacheLife: number;
    packCardSize: number;
    listMode: boolean;
    instanceLocation: string;
    verbose: boolean;
    cloudSaves: boolean;
    autoOpenChat: boolean | string;
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

export interface Resolution {
    width: number;
    height: number;
}
