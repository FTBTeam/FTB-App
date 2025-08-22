package dev.ftb.app.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.app.AppMain;
import dev.ftb.app.Constants;
import dev.ftb.app.api.WebsocketServer.PortMode;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.instances.*;
import dev.ftb.app.api.data.other.*;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.instances.*;
import dev.ftb.app.api.handlers.instances.backups.InstanceDeleteBackupHandler;
import dev.ftb.app.api.handlers.instances.backups.InstanceGetBackupsHandler;
import dev.ftb.app.api.handlers.instances.backups.InstanceRestoreBackupHandler;
import dev.ftb.app.api.handlers.other.*;
import dev.ftb.app.api.handlers.profiles.*;
import dev.ftb.app.api.handlers.storage.StorageGetAllHandler;
import dev.ftb.app.api.handlers.storage.StorageGetHandler;
import dev.ftb.app.api.handlers.storage.StoragePutHandler;
import dev.ftb.app.util.MiscUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WebSocketHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static final Map<String, Pair<Class<? extends BaseData>, IMessageHandler<? extends BaseData>>> register = new HashMap<>();

    private static @Nullable WebsocketServer server;

    static {
        register("appInit", AppInitHandler.Data.class, new AppInitHandler());
        register("moveInstances", MoveInstancesHandler.Data.class, new MoveInstancesHandler());
        register("installedInstances", InstalledInstancesData.class, new InstalledInstancesHandler());
        register("instanceCategories", InstanceCategoryHandler.Data.class, new InstanceCategoryHandler());
        register("pinInstance", PinInstanceHandler.Data.class, new PinInstanceHandler());
        register("launchInstance", LaunchInstanceData.class, new LaunchInstanceHandler());
        register("instance.kill", KillInstanceData.class, new KillInstanceHandler());
        register("installInstance", InstallInstanceData.class, new InstallInstanceHandler());
        register("cancelInstallInstance", CancelInstallInstanceData.class, new CancelInstallInstanceHandler());
        register("updateInstance", UpdateInstanceData.class, new UpdateInstanceHandler());
        register("uninstallInstance", UninstallInstanceData.class, new UninstallInstanceHandler());
        register("instanceConfigure", InstanceConfigureData.class, new InstanceConfigureHandler());
        register("instanceModToggle", InstanceModToggleData.class, new InstanceModToggleHandler());
        register("instanceOverrideModLoader", InstanceOverrideModLoaderData.class, new InstanceOverrideModLoaderHandler());
        register("instanceBrowse", BrowseInstanceData.class, new BrowseInstanceHandler());
        register("getInstanceFolders", GetInstanceFoldersHandler.Request.class, new GetInstanceFoldersHandler());
        register("duplicateInstance", DuplicateInstanceHandler.Request.class, new DuplicateInstanceHandler());
        register("getSettings", SettingsInfoData.class, new SettingsInfoHandler());
        register("saveSettings", SettingsConfigureData.class, new SettingsConfigureHandler());
        register("modalCallback", OpenModalData.ModalCallbackData.class, new ModalCallbackHandler());
        register("fileHash", FileHashData.class, new FileHashHandler()); // Not used
        register("uploadLogs", UploadLogsData.class, new UploadLogsHandler());
        register("getJavas", GetJavasData.class, new GetJavasHandler());
        register("instanceMods", InstanceModsData.class, new InstanceModsHandler());
        register("pong", PongLauncherData.class, new PongLauncherHandler());
        register("messageClient", MessageClientData.class, new MessageClientHandler()); // not really used but referenced
        register("instanceInstallMod", InstanceInstallModData.class, new InstanceInstallModHandler());
        register("setInstanceArt", SetInstanceArtData.class, new SetInstanceArtHandler());
        register("instanceVersionInfo", InstanceVersionInfoData.class, new InstanceVersionInfoHandler());

        register("instanceGetBackups", InstanceGetBackupsHandler.Request.class, new InstanceGetBackupsHandler());
        register("instanceRestoreBackup", InstanceRestoreBackupHandler.Request.class, new InstanceRestoreBackupHandler());
        register("instanceDeleteBackup", InstanceDeleteBackupHandler.Request.class, new InstanceDeleteBackupHandler());

        register("profiles.get", BaseData.class, new GetProfilesHandler());
        register("profiles.remove", RemoveProfileHandler.Data.class, new RemoveProfileHandler());
        register("profiles.setActiveProfile", SetActiveProfileHandler.Data.class, new SetActiveProfileHandler());
        register("profiles.ms.authenticate", AuthenticateMsProfileHandler.Data.class, new AuthenticateMsProfileHandler());
        register("profiles.refresh", RefreshAuthenticationProfileHandler.Data.class, new RefreshAuthenticationProfileHandler());
        register("profiles.is-valid", AccountIsValidHandler.Data.class, new AccountIsValidHandler());
        
        register("accounts.store-oauth", StoreFtbAccountHandler.Data.class, new StoreFtbAccountHandler());
        register("accounts.get-oauth", BaseData.class, new GetFtbAccountHandler());
        register("accounts.sign-out", BaseData.class, new SignOutFTBAccountHandler());
        
        register("storage.put", StoragePutHandler.Data.class, new StoragePutHandler());
        register("storage.get", StorageGetHandler.Data.class, new StorageGetHandler());
        register("storage.get-all", BaseData.class, new StorageGetAllHandler());

        register("webRequest", WebRequestData.class, new WebRequestHandler());
        register("openDebugTools", BaseData.class, new OpenDebugToolsHandler());
    }

    private static void register(String name, Class<? extends BaseData> clazz, IMessageHandler<? extends BaseData> handler) {
        register.put(name, Pair.of(clazz, handler));
    }

    public static int startWebsocket(PortMode portMode) {
        int port = switch (portMode) {
            case DYNAMIC -> MiscUtils.getRandomEphemeralPort();
            case STATIC -> Constants.WEBSOCKET_PORT;
        };
        server = new NanoHttpdWebsocketServer(port, portMode);
        server.startServer();
        return port;
    }

    public static void restartOnPort(int newPort) {
        assert server != null;
        server.stopServer();
        server = new NanoHttpdWebsocketServer(newPort, PortMode.STATIC);
        server.startServer();
    }

    public static void stopWebsocket() {
        if (server == null) return;
        server.stopServer();
    }

    public static void sendMessage(BaseData data) {
        if (server == null) return;
        server.sendMessage(data);
    }

    public static int getPort() {
        assert server != null : "Websocket not configured yet.";
        return server.getPort();
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
            BaseData parsedData = GSON.fromJson(data, entry.getLeft());
            if (AppMain.isDevMode || (parsedData.secret != null && parsedData.secret.equals(Constants.WEBSOCKET_SECRET))) {
                CompletableFuture.runAsync(() -> iMessageHandler.handle(parsedData), AppMain.taskExeggutor).exceptionally((t) -> {
                    LOGGER.error("Error handling message", t);
                    return null;
                });
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse message.", e);
        }
    }

    // TODO: Make this less hacky and ideally via a gradle task?
    public static void main(String[] args) throws IOException {
        LOGGER.info("Generating endpoints...");
        writeEndpointsToTypeFile();
    }
    
    private static void writeEndpointsToTypeFile() throws IOException {
        var runningDir = System.getProperty("user.dir");
        var outputFile = Path.of(runningDir, "..", "src", "core", "types", "javaApiEndpoints.d.ts");
        
        StringBuilder output = new StringBuilder("""
            /**
             * This file is generated by the WebSocketHandler class.
             * Do not modify this file directly.
             */
            
            """);
        
        output.append("export type ApiEndpoints = ");
        for (Map.Entry<String, Pair<Class<? extends BaseData>, IMessageHandler<? extends BaseData>>> entry : register.entrySet()) {
            output.append("\t").append("\"").append(entry.getKey()).append("\" |\n");
            if (entry.getKey().equals("pong")) {
                output.append("\t\"ping\" |\n");
            }
        }
        // Remove the last " | "
        output.delete(output.length() - 3, output.length());
        output.append(";");

        Files.writeString(outputFile, output.toString());
    }
}
