export interface ModPack {
    kind: 'modpack';
    synopsis: string;
    description: string;
    art: Art[];
    authors: Authors[];
    versions: Versions[];
    installs: number;
    plays: number;
    featured: boolean;
    refreshed: number;
    id: number;
    name: string;
    type: string;
    updated: number;
    tags: ModPackTag[];
    notification: string;
    links: ModPackLink[];
}

export interface ModPackLink {
    id: number;
    name: string;
    link: string;
    type: string;
}

export interface Instance {
    kind: string;
    id: number;
    uuid: string;
    name: string;
    versionId: number;
    version: string;
    art: string;
    path: string;
    authors: string[];
    lastPlayed: number;
    isImport: boolean;
    jvmArgs: string;
    jrePath: string;
    memory: number;
    minMemory: number;
    recMemory: number;
    width: number;
    height: number;
    notification: string;
    modpack: ModPack | undefined;
    cloudSaves: boolean;
}

export interface Art {
    width: number;
    height: number;
    url: string;
    sha1: string;
    size: number;
    id: number;
    type: string;
    updated: number;
}

export interface Authors {
    website: string;
    id: number;
    name: string;
    type: string;
    updated: number;
}

export interface Versions {
    specs: VersionSpecs[];
    id: number;
    name: string;
    type: string;
    updated: number;
    mtgID?: string;
}

export interface VersionSpecs {
    id: number;
    minimum: number;
    recommended: number;
}

export interface File {
    name: string;
    downloaded: boolean;
}

export interface InstallProgress {
    modpackID: number;
    messageID: number;
    instanceID?: string;
    progress: number;
    pack?: ModPack;
    error?: boolean;
    errorMessage?: string;
    downloadSpeed: number;
    stage: string;
    downloadedBytes: number;
    message: string;
    totalBytes: number;
    files: File[];
}

export interface ModpackState {
    installedPacks: Instance[];
    search: ModPack[];
    popularInstalls: ModPack[];
    popularPlays: ModPack[];
    featuredPacks: ModPack[];
    recentPacks: ModPack[];
    privatePacks: ModPack[];
    all: ModPack[];
    error: boolean;
    errorMsg: string;
    loading: boolean;
    installing: InstallProgress | null;
    currentModpack: ModPack | null;
    packsCache: ModPacks;
    searchString: string;
    launchProgress: Bar[] | null;
}

interface Bar {
    title: string;
    steps: number;
    step: number;
    message: string;
}

export interface ModPacks {
    [index: number]: ModPack;
}
export interface Changelog {
    content: string;
    updated: number;
    status: string;
}

export interface ModPackTag {
    id: number;
    name: string;
}
