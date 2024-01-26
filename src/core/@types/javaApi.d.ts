// @ts-nocheck

export interface BaseData {
    type: string;
    requestId: string;
    secret: string;
}

export interface BrowseInstanceData extends BaseData {
    uuid: string;
    folder: string;
}

export interface BrowseInstanceDataReply extends BaseData {
    status: string;
}

export interface CancelInstallInstanceData extends BaseData {
    uuid: string;
}

export interface CancelInstallInstanceDataReply extends BaseData {
    status: string;
    message: string;
    uuid: string;
}

export interface CheckCurseZipData extends BaseData {
    path: string;
}

export interface CheckCurseZipDataReply extends CheckCurseZipData {
    success: boolean;
    message: string;
}

export interface CheckShareCodeData extends BaseData {
    shareCode: string;
}

export interface CheckShareCodeDataReply extends CheckShareCodeData {
    success: boolean;
}

export interface CloudSavesReloadedData extends BaseData {
    changedInstances: InstanceJson[];
    removedInstances: string[];
}

export interface CloudSavesStatsData extends BaseData {
    bucketSize: number;
}

export interface InstallInstanceData extends BaseData {
    uuid: string;
    id: number;
    version: number;
    _private: boolean;
    packType: number;
    mcVersion: string;
    shareCode: string;
    importFrom: string;
    name: string;
    artPath: string;
    category: string;
    ourOwn: boolean;
    ram: number;
    fullscreen: boolean;
    cloudSaves: boolean;
    screenWidth: number;
    screenHeight: number;
}

export interface InstallInstanceDataReply extends BaseData {
    status: string;
    message: string;
    instanceData: InstanceJson;
}

export interface InstalledInstancesData extends BaseData {
    refresh: boolean;
}

export interface InstalledInstancesDataReply extends BaseData {
    instances: SugaredInstanceJson[];
    cloudInstances: any[];
    availableCategories: string[];
}

export interface InstanceCloudSyncConflictData extends BaseData {
    uuid: string;
    code: string;
    message: string;
}

export interface InstanceCloudSyncResolveConflictData extends BaseData {
    uuid: string;
    resolution: SyncDirection;
}

export interface InstanceCloudSyncResolveConflictDataReply extends BaseData {
    status: string;
    message: string;
}

export interface InstanceConfigureData extends BaseData {
    uuid: string;
    instanceJson: string;
}

export interface InstanceConfigureDataReply extends BaseData {
    errorMessage: string;
    status: string;
    instanceJson: SugaredInstanceJson;
}

export interface InstanceDisableCloudSavesData extends BaseData {
    instance: string;
}

export interface InstanceDisableCloudSavesDataReply extends BaseData {
    status: string;
    message: string;
}

export interface InstanceEnableCloudSavesData extends BaseData {
    instance: string;
}

export interface InstanceEnableCloudSavesDataReply extends BaseData {
    status: string;
    message: string;
}

export interface InstanceInstallModData extends BaseData {
    uuid: string;
    modId: number;
    versionId: number;
}

export interface PendingInstall {
    modId: number;
    versionId: number;
}

export interface InstanceInstallModDataReply extends BaseData {
    status: string;
    message: string;
    dependencies: PendingInstall[];
}

export interface InstanceModToggleData extends BaseData {
    uuid: string;
    fileId: number;
    fileName: string;
}

export interface InstanceModToggleDataReply extends InstanceModToggleData {
    successful: boolean;
}

export interface InstanceModsData extends BaseData {
    uuid: string;
    _private: boolean;
}

export interface InstanceModsDataReply extends InstanceModsData {
    files: ModInfo[];
}

export interface RichModData extends InstanceModsData {
    file: ModInfo;
    richData: CurseMetadata;
}

export interface UpdateAvailable extends InstanceModsData {
    file: ModInfo;
    update: CurseMetadata;
}

export interface UpdateCheckingFinished extends InstanceModsData {
}

export interface InstanceOverrideModLoaderData extends BaseData {
    uuid: string;
    modLoaderId: number;
    modLoaderVersion: number;
}

export interface InstanceOverrideModLoaderDataReply extends BaseData {
    status: string;
    message: string;
}

export interface InstanceVersionInfoData extends BaseData {
    uuid: string;
}

export interface InstanceVersionInfoDataReply extends BaseData {
    status: string;
    message: string;
    versionManifest: ModpackVersionManifest;
}

export interface KillInstanceData extends BaseData {
    uuid: string;
}

export interface KillInstanceDataReply extends BaseData {
    status: string;
    message: string;
}

export interface LaunchInstanceData extends BaseData {
    uuid: string;
    extraArgs: string;
    cancelLaunch: boolean;
    offline: boolean;
    offlineUsername: string;
}

export interface Logs extends BaseData {
    uuid: string;
    messages: string[];
}

export interface LaunchInstanceDataReply extends BaseData {
    status: string;
    message: string;
}

export interface Status extends BaseData {
    step: number;
    totalSteps: number;
    stepProgress: number;
    stepDesc: string;
    stepProgressHuman: string;
}

export interface Stopped extends BaseData {
    instanceId: string;
    status: string;
    exitCode: number;
}

export interface MessageClientData extends BaseData {
    uuid: string;
    message: string;
}

export interface OperationProgressUpdateData extends BaseData {
    operation: string;
    metadata: { [index: string]: string };
    stage: Stage;
    steps: number;
    totalSteps: number;
    percent: number;
    speed: number;
    bytes: number;
    totalBytes: number;
}

export interface PollCloudInstancesData extends BaseData {
}

export interface PollCloudInstancesDataReply extends BaseData {
    status: string;
    message: string;
}

export interface SetInstanceArtData extends BaseData {
    uuid: string;
    artPath: string;
}

export interface SetInstanceArtDataReply extends BaseData {
    status: string;
    message: string;
}

export interface ShareInstanceData extends BaseData {
    uuid: string;
}

export interface ShareInstanceDataReply extends BaseData {
    status: string;
    message: string;
    uuid: string;
    code: string;
}

export interface SyncCloudInstanceData extends BaseData {
    uuid: string;
}

export interface SyncCloudInstanceDataReply extends BaseData {
    status: string;
    message: string;
}

export interface UninstallInstanceData extends BaseData {
    uuid: string;
}

export interface UninstallInstanceDataReply extends BaseData {
    status: string;
    message: string;
}

export interface UpdateInstanceData extends InstallInstanceData {
}

export interface ClientLaunchData extends BaseData {
}

export interface ClientLaunchDataReply extends BaseData {
    messageType: string;
    message: string;
    instance: string;
    clientData: any;
}

export interface CloseModalData extends BaseData {
}

export interface FileHashData extends BaseData {
    uuid: string;
    filePath: string;
}

export interface FileHashDataReply extends BaseData {
    md5Hash: string;
    shaHash: string;
}

export interface GetJavasData extends BaseData {
}

export interface JavaInstall {
    name: string;
    path: string;
}

export interface GetJavasDataReply extends BaseData {
    javas: JavaInstall[];
}

export interface OpenModalData extends BaseData {
    title: string;
    message: string;
    buttons: ModalButton[];
    id: string;
}

export interface ModalButton {
    message: string;
    name: string;
    type: string;
}

export interface ModalCallbackData extends BaseData {
    id: string;
    message: string;
}

export interface PingLauncherData extends BaseData {
}

export interface PongLauncherData extends BaseData {
}

export interface SettingsConfigureData extends BaseData {
    settings: SettingsData;
}

export interface SettingsConfigureDataReply extends BaseData {
    status: string;
}

export interface SettingsInfoData extends BaseData {
}

export interface SettingsInfoDataReply extends SettingsInfoData {
    settingsInfo: SettingsData;
    totalMemory: number;
    availableMemory: number;
    totalCores: number;
    autoResolution: Dimension;
    supportedResolutions: Dimension[];
}

export interface UploadLogsData extends BaseData {
}

export interface UploadLogsDataReply extends BaseData {
    error: boolean;
    code: string;
}

export interface WebRequestData extends BaseData {
    url: string;
    method: string;
    headers: { [index: string]: string[] };
    body: Body;
}

export interface Body {
    contentType: string;
    bytes: any;
}

export interface WebRequestDataResponse extends BaseData {
    status: string;
    statusMessage: string;
    statusCode: number;
    statusLine: string;
    headers: { [index: string]: string[] };
    body: Body;
}

export interface DuplicateInstanceHandlerReply extends DuplicateInstanceHandlerRequest {
    message: string;
    success: boolean;
    instance: SugaredInstanceJson;
}

export interface DuplicateInstanceHandlerRequest extends BaseData {
    uuid: string;
    newName: string;
}

export interface GetInstanceFoldersHandlerReply extends GetInstanceFoldersHandlerRequest {
    folders: string[];
    success: boolean;
}

export interface GetInstanceFoldersHandlerRequest extends BaseData {
    uuid: string;
}

export interface SugaredInstanceJson extends InstanceJson {
    path: string;
    pendingCloudInstance: boolean;
    rootDirs: string[];
}

export interface MoveInstancesHandlerData extends BaseData {
    newLocation: string;
}

export interface MoveInstancesHandlerReply extends MoveInstancesHandlerData {
    state: string;
    error: string;
}

export interface InstanceDeleteBackupHandlerReply extends InstanceDeleteBackupHandlerRequest {
    message: string;
    success: boolean;
}

export interface InstanceDeleteBackupHandlerRequest extends BaseData {
    backupLocation: string;
}

export interface InstanceGetBackupsHandlerReply extends InstanceGetBackupsHandlerRequest {
    backups: Backup[];
}

export interface InstanceGetBackupsHandlerRequest extends BaseData {
    uuid: string;
}

export interface InstanceRestoreBackupHandlerReply extends InstanceRestoreBackupHandlerRequest {
    message: string;
    success: boolean;
}

export interface InstanceRestoreBackupHandlerRequest extends BaseData {
    uuid: string;
    backupLocation: string;
}

export interface AppInitHandlerData extends BaseData {
}

export interface AppInitHandlerReply extends AppInitHandlerData {
    success: boolean;
    errorMessage: string;
    basicData: BasicDataAndAccount;
    profile: MineTogetherProfile;
    apiCredentials: UserApiCredentials;
}

export interface VideoCacheHandlerData extends BaseData {
    url: string;
    fileName: string;
}

export interface VideoCacheHandlerReply extends VideoCacheHandlerData {
    location: string;
}

export interface MineTogetherAuthenticationHandlerData extends BaseData {
    authType: string;
    apiKey: string;
    appToken: string;
}

export interface MineTogetherAuthenticationHandlerReply extends MineTogetherAuthenticationHandlerData {
    success: boolean;
    message: string;
    basicData: BasicDataAndAccount;
    profile: MineTogetherProfile;
}

export interface MineTogetherLogoutHandlerReply extends BaseData {
    success: boolean;
}

export interface AccountIsValidHandlerData extends BaseData {
    profileUuid: string;
}

export interface AccountIsValidHandlerReply extends AccountIsValidHandlerData {
    success: boolean;
    response: string;
}

export interface AuthenticateMsProfileHandlerData extends BaseData {
    liveAccessToken: string;
    liveRefreshToken: string;
    liveExpires: number;
}

export interface AuthenticateMsProfileHandlerReply extends AuthenticateMsProfileHandlerData {
    success: boolean;
    networkError: boolean;
    code: string;
}

export interface GetProfilesHandlerReply extends BaseData {
    profiles: AccountProfile[];
    activeProfile: AccountProfile;
}

export interface RefreshAuthenticationProfileHandlerData extends BaseData {
    profileUuid: string;
    liveAccessToken: string;
    liveRefreshToken: string;
    liveExpires: number;
}

export interface RefreshAuthenticationProfileHandlerReply extends RefreshAuthenticationProfileHandlerData {
    networkError: boolean;
    success: boolean;
    code: string;
}

export interface RemoveProfileHandlerData extends BaseData {
    uuid: string;
}

export interface RemoveProfileHandlerReply extends RemoveProfileHandlerData {
    success: boolean;
}

export interface SetActiveProfileHandlerData extends BaseData {
    uuid: string;
}

export interface SetActiveProfileHandlerReply extends BaseData {
    success: boolean;
}

export interface StorageGetAllHandlerReply extends BaseData {
    response: string;
}

export interface StorageGetHandlerData extends BaseData {
    key: string;
}

export interface StorageGetHandlerReply extends StorageGetHandlerData {
    response: string;
}

export interface StoragePutHandlerData extends BaseData {
    key: string;
    value: string;
}

export interface StoragePutHandlerReply extends StoragePutHandlerData {
    success: boolean;
}

export interface SettingsData {
    spec: string;
    instanceLocation: string;
    general: GeneralSettings;
    instanceDefaults: InstanceSettings;
    appearance: AppearanceSettings;
    proxy: ProxySettings;
    download: DownloadSettings;
}

export interface InstanceJson {
    uuid: string;
    id: number;
    versionId: number;
    name: string;
    version: string;
    mcVersion: string;
    /**
     * @deprecated
     */
    minMemory: number;
    /**
     * @deprecated
     */
    recMemory: number;
    memory: number;
    jvmArgs: string;
    shellArgs: string;
    embeddedJre: boolean;
    jrePath: string;
    width: number;
    height: number;
    fullscreen: boolean;
    modLoader: string;
    isModified: boolean;
    isImport: boolean;
    cloudSaves: boolean;
    hasInstMods: boolean;
    installComplete: boolean;
    category: string;
    releaseChannel: string;
    locked: boolean;
    packType: number;
    _private: boolean;
    totalPlayTime: number;
    lastPlayed: number;
    /**
     * @deprecated
     */
    art: string;
}

export interface ModInfo {
    fileId: number;
    fileName: string;
    version: string;
    enabled: boolean;
    size: number;
    sha1: string;
    curse: CurseMetadata;
}

export interface CurseMetadata {
    type: Type;
    curseProject: number;
    curseFile: number;
    name: string;
    synopsis: string;
    icon: string;
}

export interface ModpackVersionManifest {
    status: string;
    message: string;
    id: number;
    parent: number;
    name: string;
    specs: Specs;
    type: string;
    targets: Target[];
    files: ModpackFile[];
    installs: number;
    plays: number;
    updated: number;
    refreshed: number;
}

export interface Stage {
}

export interface Dimension {
    width: number;
    height: number;
}

export interface BasicDataAndAccount {
    data: BasicProfileData;
    account: MineTogetherAccount;
}

export interface MineTogetherProfile {
    hash: Hashes;
    friendCode: string;
    chat: Chat;
    cached: boolean;
    display: string;
    hasAccount: boolean;
    premium: boolean;
}

export interface UserApiCredentials {
    apiUrl: string;
    apiSecret: string;
    settings: ApiSettings;
}

export interface AccountProfile {
    isMicrosoft: boolean;
    uuid: string;
    lastLogin: number;
    username: string;
    msAuth: MSAuthStore;
    mcAuth: YggdrasilAuthStore;
    skins: AccountSkin[];
}

export interface GeneralSettings {
    releaseChannel: string;
    cacheLife: number;
    exitOverwolf: boolean;
    verbose: boolean;
}

export interface InstanceSettings {
    width: number;
    height: number;
    memory: number;
    fullscreen: boolean;
    updateChannel: string;
    javaArgs: string;
    shellArgs: string;
}

export interface AppearanceSettings {
    useSystemWindowStyle: boolean;
    showAds: boolean;
}

export interface ProxySettings {
    type: string;
    host: string;
    port: number;
    username: string;
    password: string;
}

export interface DownloadSettings {
    threadLimit: number;
    speedLimit: number;
}

export interface Specs {
    id: number;
    minimum: number;
    recommended: number;
}

export interface Backup {
    worldName: string;
    createTime: number;
    backupLocation: string;
    size: number;
    ratio: number;
    sha1: string;
    preview: string;
    snapshot: boolean;
}

export interface BasicProfileData {
    uuid: string;
    modpacksToken: string;
    s3Credentials: S3Credentials;
}

export interface MineTogetherAccount {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    attributes: { [index: string]: string[] };
    accounts: Account[];
    mtHash: string;
    fullMTHash: string;
    activePlan: ActivePlan;
}

export interface Hashes {
    long: string;
}

export interface Chat {
    hash: ChatHash;
    online: boolean;
}

export interface ApiSettings {
    useAuthorizationHeader: boolean;
    useAuthorizationAsBearer: boolean;
    usePublicUrl: boolean;
}

export interface MSAuthStore {
    minecraftUuid: string;
    minecraftToken: string;
    xblUserHash: string;
    liveAccessToken: string;
    liveRefreshToken: string;
    liveExpiresAt: number;
}

export interface YggdrasilAuthStore {
    clientToken: string;
    accessToken: string;
}

export interface Target {
    id: number;
    version: string;
    name: string;
    type: string;
    updated: number;
}

export interface ModpackFile {
    id: number;
    version: string;
    path: string;
    name: string;
    url: string;
    mirror: string[];
    sha1: any;
    size: number;
    clientonly: boolean;
    serveronly: boolean;
    optional: boolean;
    type: string;
    tags: any;
    updated: number;
}

export interface S3Credentials {
    accessKeyId: string;
    secretAccessKey: string;
    bucket: string;
    host: string;
}

export interface ActivePlan {
    id: number;
    orderid: number;
    pid: number;
    name: string;
    paymentMethod: string;
    paymentMethodActual: string;
    nextDueDate: string;
    status: string;
    customFields: CustomFields;
}

export interface ChatHash {
    medium: string;
    hashShort: string;
}

export interface AccountSkin {
    id: string;
    state: string;
    url: string;
    variant: string;
    alias: string;
}

export interface Account {
    identityProvider: string;
    userId: string;
    userName: string;
    mcName: string;
    accountKey: string;
    accountSecret: string;
}

export interface CustomFields {
    customfield: Customfield[];
}

export interface Customfield {
    id: number;
    name: string;
    translatedName: string;
    value: string;
}

export type SyncDirection = "UP_TO_DATE" | "DOWNLOAD" | "UPLOAD";

export type Type = "BASIC" | "FULL";
