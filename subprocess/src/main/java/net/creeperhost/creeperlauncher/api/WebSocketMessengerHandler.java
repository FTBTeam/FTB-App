package net.creeperhost.creeperlauncher.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.data.friends.AddFriendData;
import net.creeperhost.creeperlauncher.api.data.friends.BlockFriendData;
import net.creeperhost.creeperlauncher.api.data.friends.GetFriendsData;
import net.creeperhost.creeperlauncher.api.data.instances.*;
import net.creeperhost.creeperlauncher.api.data.irc.IRCConnectData;
import net.creeperhost.creeperlauncher.api.data.irc.IRCQuitRequestData;
import net.creeperhost.creeperlauncher.api.data.irc.IRCSendMessageData;
import net.creeperhost.creeperlauncher.api.data.other.*;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.api.handlers.friends.AddFriendHandler;
import net.creeperhost.creeperlauncher.api.handlers.friends.BlockFriendHandler;
import net.creeperhost.creeperlauncher.api.handlers.friends.GetFriendsHandler;
import net.creeperhost.creeperlauncher.api.handlers.instances.*;
import net.creeperhost.creeperlauncher.api.handlers.instances.backups.InstanceDeleteBackupHandler;
import net.creeperhost.creeperlauncher.api.handlers.instances.backups.InstanceGetBackupsHandler;
import net.creeperhost.creeperlauncher.api.handlers.instances.backups.InstanceRestoreBackupHandler;
import net.creeperhost.creeperlauncher.api.handlers.irc.IRCConnectHandler;
import net.creeperhost.creeperlauncher.api.handlers.irc.IRCQuitRequestHandler;
import net.creeperhost.creeperlauncher.api.handlers.irc.IRCSendMessageHandler;
import net.creeperhost.creeperlauncher.api.handlers.other.*;
import net.creeperhost.creeperlauncher.api.handlers.profiles.*;
import net.creeperhost.creeperlauncher.api.handlers.storage.StorageGetAllHandler;
import net.creeperhost.creeperlauncher.api.handlers.storage.StorageGetHandler;
import net.creeperhost.creeperlauncher.api.handlers.storage.StoragePutHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WebSocketMessengerHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, Pair<Class<? extends BaseData>, IMessageHandler<? extends BaseData>>> register = new HashMap<>();

    static Gson gson = new Gson();

    static {
        register("installedInstances", InstalledInstancesData.class, new InstalledInstancesHandler());
        register("launchInstance", LaunchInstanceData.class, new LaunchInstanceHandler());
        register("instance.kill", KillInstanceData.class, new KillInstanceHandler());
        register("installInstance", InstallInstanceData.class, new InstallInstanceHandler());
        register("cancelInstallInstance", CancelInstallInstanceData.class, new CancelInstallInstanceHandler());
        register("updateInstance", UpdateInstanceData.class, new UpdateInstanceHandler());
        register("uninstallInstance", UninstallInstanceData.class, new UninstallInstanceHandler());
        register("instanceConfigure", InstanceConfigureData.class, new InstanceConfigureHandler());
        register("instanceModToggle", InstanceModToggleData.class, new InstanceModToggleHandler());
        register("instanceBrowse", BrowseInstanceData.class, new BrowseInstanceHandler());
        register("getInstanceFolders", GetInstanceFoldersHandler.Request.class, new GetInstanceFoldersHandler());
        register("duplicateInstance", DuplicateInstanceHandler.Request.class, new DuplicateInstanceHandler());
        register("getSettings", SettingsInfoData.class, new SettingsInfoHandler());
        register("saveSettings", SettingsConfigureData.class, new SettingsConfigureHandler());
        register("modalCallback", OpenModalData.ModalCallbackData.class, new ModalCallbackHandler());
        register("fileHash", FileHashData.class, new FileHashHandler()); // Not used
        register("storeAuthDetails", StoreAuthDetailsData.class, new StoreAuthDetailsHandler());
        register("syncInstance", SyncCloudInstanceData.class, new SyncCloudInstanceHandler());
        register("ircConnect", IRCConnectData.class, new IRCConnectHandler());
        register("ircSendMessage", IRCSendMessageData.class, new IRCSendMessageHandler());
        register("ircQuitRequest", IRCQuitRequestData.class, new IRCQuitRequestHandler());
        register("uploadLogs", UploadLogsData.class, new UploadLogsHandler());
        register("getJavas", GetJavasData.class, new GetJavasHandler());
        register("getFriends", GetFriendsData.class, new GetFriendsHandler());
        register("blockFriend", BlockFriendData.class, new BlockFriendHandler());
        register("addFriend", AddFriendData.class, new AddFriendHandler());
        register("instanceMods", InstanceModsData.class, new InstanceModsHandler());
        register("instanceEnableCloudSaves", InstanceEnableCloudSavesData.class, new InstanceEnableCloudSavesHandler());
        register("instanceDisableCloudSaves", InstanceDisableCloudSavesData.class, new InstanceDisableCloudSavesHandler());
        register("yeetLauncher", YeetLauncherData.class, new YeetLauncherHandler());
        register("pong", PongLauncherData.class, new PongLauncherHandler());
        register("ping", PingLauncherData.class);
        register("messageClient", MessageClientData.class, new MessageClientHandler()); // not really used but referenced
        register("shareInstance", ShareInstanceData.class, new ShareInstanceHandler());
        register("instanceInstallMod", InstanceInstallModData.class, new InstanceInstallModHandler());
        register("setInstanceArt", SetInstanceArtData.class, new SetInstanceArtHandler());
        register("instanceVersionInfo", InstanceVersionInfoData.class, new InstanceVersionInfoHandler());

        register("instanceGetBackups", InstanceGetBackupsHandler.Request.class, new InstanceGetBackupsHandler());
        register("instanceRestoreBackup", InstanceRestoreBackupHandler.Request.class, new InstanceRestoreBackupHandler());
        register("instanceDeleteBackup", InstanceDeleteBackupHandler.Request.class, new InstanceDeleteBackupHandler());

        register("pollCloudInstances", PollCloudInstancesData.class, new PollCloudInstancesHandler());
        register("resolveSyncConflict", InstanceCloudSyncResolveConflictData.class, new InstanceCloudSyncResolveConflictHandler());

        register("checkShareCode", CheckShareCodeData.class, new CheckShareCode());
        register("checkCurseZip", CheckCurseZipData.class, new CheckCurseZip());

        register("profiles.get", BaseData.class, new GetProfilesHandler());
        register("profiles.remove", RemoveProfileHandler.Data.class, new RemoveProfileHandler());
        register("profiles.setActiveProfile", SetActiveProfileHandler.Data.class, new SetActiveProfileHandler());
        register("profiles.mc.authenticate", AuthenticateMcProfileHandler.Data.class, new AuthenticateMcProfileHandler());
        register("profiles.ms.authenticate", AuthenticateMsProfileHandler.Data.class, new AuthenticateMsProfileHandler());
        register("profiles.refresh", RefreshAuthenticationProfileHandler.Data.class, new RefreshAuthenticationProfileHandler());
        register("profiles.is-valid", AccountIsValidHandler.Data.class, new AccountIsValidHandler());

        register("storage.put", StoragePutHandler.Data.class, new StoragePutHandler());
        register("storage.get", StorageGetHandler.Data.class, new StorageGetHandler());
        register("storage.get-all", BaseData.class, new StorageGetAllHandler());

        register("webRequest", WebRequestData.class, new WebRequestHandler());

        register("openDebugTools", BaseData.class, new OpenDebugToolsHandler());
    }

    public static void register(String name, Class<? extends BaseData> clazz, IMessageHandler<? extends BaseData> handler) {
        register.put(name, Pair.of(clazz, handler));
    }

    public static void register(String name, Class<? extends BaseData> clazz) {
        register.put(name, Pair.of(clazz, null));
    }

    public static void handleMessage(String data) {
        JsonElement parse = JsonParser.parseString(data);
        if (!parse.isJsonObject()) return;

        JsonObject jsonObject = parse.getAsJsonObject();
        if (!jsonObject.has("type")) return;

        String type = jsonObject.get("type").getAsString();
        Pair<Class<? extends BaseData>, IMessageHandler<? extends BaseData>> entry = register.get(type);
        IMessageHandler<? extends BaseData> iMessageHandler = entry.getRight();
        if (iMessageHandler == null) {
            LOGGER.error("No handler for message type '{}'", type);
            return;
        }

        try {
            BaseData parsedData = gson.fromJson(data, entry.getLeft());
            if (CreeperLauncher.isDevMode || (parsedData.secret != null && parsedData.secret.equals(CreeperLauncher.websocketSecret))) {
                CompletableFuture.runAsync(() -> iMessageHandler.handle(parsedData), CreeperLauncher.taskExeggutor).exceptionally((t) -> {
                    LOGGER.error("Error handling message", t);
                    return null;
                });
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse message.", e);
        }
    }
}
