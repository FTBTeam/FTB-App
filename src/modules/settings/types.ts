export interface Settings {
    width: number;
    height: number;
    memory: number;
    keepLauncherOpen: boolean;
    jvmargs: string;
    enableAnalytics: boolean;
    enableChat: boolean;
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
    blockedUsers: string[] | string;
    sessionString?: string;
    mtConnect: boolean;
    automateMojang: boolean;
    showAdverts: boolean | string;
    loadInApp: boolean | string;
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
    javaInstalls: JavaVersions;
}

export interface JavaVersions {
    [index: string]: string;
}

export interface Resolution {
    width: number;
    height: number;
}
