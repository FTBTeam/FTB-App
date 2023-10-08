import {
  AccountIsValidHandlerData,
  AccountIsValidHandlerReply,
  AppInitHandlerData,
  AppInitHandlerReply,
  AuthenticateMsProfileHandlerData,
  AuthenticateMsProfileHandlerReply,
  BaseData,
  BrowseInstanceData,
  BrowseInstanceDataReply,
  CancelInstallInstanceData,
  CancelInstallInstanceDataReply,
  CheckCurseZipData,
  CheckCurseZipDataReply,
  CheckShareCodeData,
  CheckShareCodeDataReply,
  DuplicateInstanceHandlerReply,
  DuplicateInstanceHandlerRequest,
  FileHashData,
  FileHashDataReply,
  GetInstanceFoldersHandlerReply,
  GetInstanceFoldersHandlerRequest,
  GetJavasData,
  GetJavasDataReply,
  GetProfilesHandlerReply,
  ImportProviderImportHandlerData, ImportProviderImportHandlerReply,
  ImportProviderInstancesHandlerData,
  ImportProviderInstancesHandlerReply,
  InstalledInstancesData,
  InstalledInstancesDataReply,
  InstallInstanceData,
  InstallInstanceDataReply,
  InstanceConfigureData,
  InstanceConfigureDataReply,
  InstanceDeleteBackupHandlerReply,
  InstanceDeleteBackupHandlerRequest,
  InstanceDisableCloudSavesData,
  InstanceDisableCloudSavesDataReply,
  InstanceEnableCloudSavesData,
  InstanceEnableCloudSavesDataReply,
  InstanceGetBackupsHandlerReply,
  InstanceGetBackupsHandlerRequest,
  InstanceInstallModData,
  InstanceInstallModDataReply,
  InstanceModsData,
  InstanceModsDataReply,
  InstanceModToggleData,
  InstanceModToggleDataReply,
  InstanceOverrideModLoaderData,
  InstanceOverrideModLoaderDataReply,
  InstanceRestoreBackupHandlerReply,
  InstanceRestoreBackupHandlerRequest,
  InstanceVersionInfoData,
  InstanceVersionInfoDataReply,
  KillInstanceData,
  KillInstanceDataReply,
  LaunchInstanceData,
  LaunchInstanceDataReply,
  MessageClientData,
  MineTogetherAuthenticationHandlerData,
  MineTogetherAuthenticationHandlerReply,
  MineTogetherLogoutHandlerReply,
  ModalCallbackData,
  MoveInstancesHandlerData,
  MoveInstancesHandlerReply,
  PingLauncherData,
  PinInstanceHandlerData,
  PinInstanceHandlerReply,
  PollCloudInstancesData,
  PollCloudInstancesDataReply,
  PongLauncherData,
  RefreshAuthenticationProfileHandlerData,
  RefreshAuthenticationProfileHandlerReply,
  RemoveProfileHandlerData,
  RemoveProfileHandlerReply,
  SetActiveProfileHandlerData,
  SetActiveProfileHandlerReply,
  SetInstanceArtData,
  SetInstanceArtDataReply,
  SettingsConfigureData,
  SettingsConfigureDataReply,
  SettingsInfoData,
  SettingsInfoDataReply,
  ShareInstanceData,
  ShareInstanceDataReply,
  StorageGetAllHandlerReply,
  StorageGetHandlerData,
  StorageGetHandlerReply,
  StoragePutHandlerData,
  StoragePutHandlerReply,
  SyncCloudInstanceData,
  SyncCloudInstanceDataReply,
  UninstallInstanceData,
  UninstallInstanceDataReply,
  UpdateInstanceData,
  UploadLogsData,
  UploadLogsDataReply,
  VideoCacheHandlerData,
  VideoCacheHandlerReply,
  WebRequestData,
  WebRequestDataResponse
} from '@/core/@types/javaApi';
import {Nullable} from '@/core/websockets/websocketsApi';

export type MessageType =
  "installedInstances" |
  "pinInstance" |
  "launchInstance" |
  "instance.kill" |
  "installInstance" |
  "instanceOverrideModLoader" |
  "cancelInstallInstance" |
  "updateInstance" |
  "uninstallInstance" |
  "instanceConfigure" |
  "instanceModToggle" |
  "instanceBrowse" |
  "getInstanceFolders" |
  "duplicateInstance" |
  "getSettings" |
  "saveSettings" |
  "modalCallback" |
  "fileHash" |
  "syncInstance" |
  "uploadLogs" |
  "getJavas" |
  "instanceMods" |
  "pong" |
  "ping" |
  "messageClient" |
  "shareInstance" |
  "instanceInstallMod" |
  "setInstanceArt" |
  "instanceVersionInfo" |
  "instanceGetBackups" |
  "instanceRestoreBackup" |
  "instanceDeleteBackup" |
  "pollCloudInstances" |
  "checkShareCode" |
  "checkCurseZip" |
  "profiles.get" |
  "profiles.remove" |
  "profiles.setActiveProfile" |
  "profiles.ms.authenticate" |
  "instanceEnableCloudSaves" |
  "instanceDisableCloudSaves" |
  "profiles.refresh" |
  "profiles.is-valid" |
  "storage.put" |
  "storage.get" |
  "storage.get-all" |
  "webRequest" |
  "openDebugTools" |
  "import.providerInstances" |
  "import.providerImport" |
  "videoCache" |
  "moveInstances" | 
  "minetogetherAuthentication" |
  "minetogetherLogoutHandler" |
  "appInit"

export type EmptyMessageResponse = {}

export type MessagePayload = {
  "appInit": {
    input: AppInitHandlerData,
    output: AppInitHandlerReply
  },
  "pinInstance": {
    input: PinInstanceHandlerData,
    output: PinInstanceHandlerReply
  }
  "minetogetherLogoutHandler": {
    input: BaseData,
    output: MineTogetherLogoutHandlerReply
  },
  "installedInstances": {
    input: InstalledInstancesData,
    output: InstalledInstancesDataReply
  },
  "launchInstance": {
    input: Nullable<LaunchInstanceData, "cancelLaunch">,
    output: LaunchInstanceDataReply
  }
  "instance.kill": {
    input: KillInstanceData,
    output: KillInstanceDataReply
  }
  "installInstance": {
    input: Nullable<InstallInstanceData, "importFrom" | "name" | "artPath">,
    output: InstallInstanceDataReply
  }
  "cancelInstallInstance": {
    input: CancelInstallInstanceData,
    output: CancelInstallInstanceDataReply
  }
  "updateInstance": {
    input: UpdateInstanceData,
    output: InstallInstanceDataReply
  }
  "uninstallInstance": {
    input: UninstallInstanceData,
    output: UninstallInstanceDataReply
  }
  "instanceConfigure": {
    input: InstanceConfigureData,
    output: InstanceConfigureDataReply
  }
  "instanceModToggle": {
    input: InstanceModToggleData,
    output: InstanceModToggleDataReply
  }
  "instanceBrowse": {
    input: Nullable<BrowseInstanceData, "folder">,
    output: BrowseInstanceDataReply
  }
  "getInstanceFolders": {
    input: GetInstanceFoldersHandlerRequest,
    output: GetInstanceFoldersHandlerReply
  }
  "duplicateInstance": {
    input: DuplicateInstanceHandlerRequest,
    output: DuplicateInstanceHandlerReply
  }
  "getSettings": {
    input: SettingsInfoData,
    output: SettingsInfoDataReply
  }
  "saveSettings": {
    input: SettingsConfigureData,
    output: SettingsConfigureDataReply
  }
  "modalCallback": {
    input: ModalCallbackData,
    output: EmptyMessageResponse
  }
  "fileHash": {
    input: FileHashData,
    output: FileHashDataReply
  }
  "syncInstance": {
    input: SyncCloudInstanceData,
    output: SyncCloudInstanceDataReply
  }
  "uploadLogs": {
    input: UploadLogsData,
    output: UploadLogsDataReply
  }
  "getJavas": {
    input: GetJavasData,
    output: GetJavasDataReply
  }
  "instanceMods": {
    input: InstanceModsData,
    output: InstanceModsDataReply
  }
  "pong": {
    input: PongLauncherData
    output: EmptyMessageResponse
  }
  "ping": {
    input: PingLauncherData
    output: EmptyMessageResponse
  }
  "messageClient": {
    input: MessageClientData
    output: EmptyMessageResponse
  }
  "shareInstance": {
    input: ShareInstanceData,
    output: ShareInstanceDataReply
  }
  "instanceInstallMod": {
    // Also supports progress
    input: InstanceInstallModData,
    output: InstanceInstallModDataReply
  }
  "setInstanceArt": {
    input: SetInstanceArtData,
    output: SetInstanceArtDataReply
  }
  "instanceVersionInfo": {
    input: InstanceVersionInfoData,
    output: InstanceVersionInfoDataReply
  }
  "instanceGetBackups": {
    input: InstanceGetBackupsHandlerRequest,
    output: InstanceGetBackupsHandlerReply
  }
  "instanceRestoreBackup": {
    input: InstanceRestoreBackupHandlerRequest,
    output: InstanceRestoreBackupHandlerReply
  }
  "instanceDeleteBackup": {
    input: InstanceDeleteBackupHandlerRequest,
    output: InstanceDeleteBackupHandlerReply
  }
  "pollCloudInstances": {
    input: PollCloudInstancesData,
    output: PollCloudInstancesDataReply
  }
  "checkShareCode": {
    input: CheckShareCodeData,
    output: CheckShareCodeDataReply
  }
  "checkCurseZip": {
    input: CheckCurseZipData
    output: CheckCurseZipDataReply
  }
  "profiles.get": {
    input: BaseData,
    output: GetProfilesHandlerReply
  }
  "profiles.remove": {
    input: RemoveProfileHandlerData,
    output: RemoveProfileHandlerReply
  }
  "profiles.setActiveProfile": {
    input: SetActiveProfileHandlerData,
    output: SetActiveProfileHandlerReply
  }
  "profiles.ms.authenticate": {
    input: AuthenticateMsProfileHandlerData,
    output: AuthenticateMsProfileHandlerReply
  }
  "profiles.refresh": {
    input: Nullable<RefreshAuthenticationProfileHandlerData, "liveExpires" | "liveAccessToken" | "liveRefreshToken">,
    output: RefreshAuthenticationProfileHandlerReply
  }
  "profiles.is-valid": {
    input: AccountIsValidHandlerData,
    output: AccountIsValidHandlerReply
  }
  "storage.put": {
    input: StoragePutHandlerData,
    output: StoragePutHandlerReply
  }
  "storage.get": {
    input: StorageGetHandlerData,
    output: StorageGetHandlerReply
  }
  "storage.get-all": {
    input: BaseData,
    output: StorageGetAllHandlerReply
  }
  "webRequest": {
    input: Nullable<WebRequestData, "body">,
    output: WebRequestDataResponse
  }
  "openDebugTools": {
    input: BaseData,
    output: EmptyMessageResponse
  },
  "instanceEnableCloudSaves": {
    input: InstanceEnableCloudSavesData,
    output: InstanceEnableCloudSavesDataReply
  },
  "instanceDisableCloudSaves": {
    input: InstanceDisableCloudSavesData,
    output: InstanceDisableCloudSavesDataReply
  },
  "instanceOverrideModLoader": {
    input: InstanceOverrideModLoaderData,
    output: InstanceOverrideModLoaderDataReply
  },
  "videoCache": {
    input: VideoCacheHandlerData,
    output: VideoCacheHandlerReply
  },
  "moveInstances": {
    input: MoveInstancesHandlerData
    output: MoveInstancesHandlerReply
  },
  "import.providerInstances": {
    input: ImportProviderInstancesHandlerData,
    output: ImportProviderInstancesHandlerReply
  },
  "import.providerImport": {
    input: ImportProviderImportHandlerData,
    output: ImportProviderImportHandlerReply
  },
  "minetogetherAuthentication": {
    input: MineTogetherAuthenticationHandlerData,
    output: Nullable<MineTogetherAuthenticationHandlerReply, "basicData" | "profile">
  }
}
