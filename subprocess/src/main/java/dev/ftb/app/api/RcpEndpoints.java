package dev.ftb.app.api;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.instances.*;
import dev.ftb.app.api.data.other.*;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.instances.*;
import dev.ftb.app.api.handlers.other.*;
import dev.ftb.app.api.handlers.profiles.*;
import dev.ftb.app.api.handlers.purge.PurgeHandler;
import dev.ftb.app.api.handlers.storage.StorageGetAllHandler;
import dev.ftb.app.api.handlers.storage.StorageGetHandler;
import dev.ftb.app.api.handlers.storage.StoragePutHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RcpEndpoints {
    private static final Map<String, Pair<Class<? extends BaseData>, IMessageHandler<? extends BaseData>>> register = new HashMap<>();

    public RcpEndpoints() {
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
        register("getInstanceFolders", GetInstanceFoldersHandler.Request.class, new GetInstanceFoldersHandler());
        register("duplicateInstance", DuplicateInstanceHandler.Request.class, new DuplicateInstanceHandler());
        register("getSettings", SettingsInfoData.class, new SettingsInfoHandler());
        register("saveSettings", SettingsConfigureData.class, new SettingsConfigureHandler());
        register("fileHash", FileHashData.class, new FileHashHandler()); // Not used
        register("uploadLogs", UploadLogsData.class, new UploadLogsHandler());
        register("getJavas", GetJavasData.class, new GetJavasHandler());
        register("instanceMods", InstanceModsData.class, new InstanceModsHandler());
        register("pong", PongLauncherData.class, new PongLauncherHandler());
        register("instanceInstallMod", InstanceInstallModData.class, new InstanceInstallModHandler());
        register("setInstanceArt", SetInstanceArtData.class, new SetInstanceArtHandler());
        register("instanceVersionInfo", InstanceVersionInfoData.class, new InstanceVersionInfoHandler());

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

        register("purge", PurgeHandler.Data.class, new PurgeHandler());
    }

    private void register(String name, Class<? extends BaseData> clazz, IMessageHandler<? extends BaseData> handler) {
        register.put(name, Pair.of(clazz, handler));
    }
    
    public Pair<Class<? extends BaseData>, IMessageHandler<? extends BaseData>> fromName(String name) {
        return register.get(name);
    }

    // TODO: Make this less hacky and ideally via a gradle task?
    public static void main(String[] args) throws IOException {
        System.out.println("Generating endpoints...");
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
