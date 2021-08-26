package net.creeperhost.creeperlauncher.api;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.data.*;
import net.creeperhost.creeperlauncher.api.data.friends.AddFriendData;
import net.creeperhost.creeperlauncher.api.data.friends.BlockFriendData;
import net.creeperhost.creeperlauncher.api.data.friends.GetFriendsData;
import net.creeperhost.creeperlauncher.api.data.instances.*;
import net.creeperhost.creeperlauncher.api.data.irc.*;
import net.creeperhost.creeperlauncher.api.data.other.*;
import net.creeperhost.creeperlauncher.api.handlers.*;
import net.creeperhost.creeperlauncher.api.handlers.friends.AddFriendHandler;
import net.creeperhost.creeperlauncher.api.handlers.friends.BlockFriendHandler;
import net.creeperhost.creeperlauncher.api.handlers.friends.GetFriendsHandler;
import net.creeperhost.creeperlauncher.api.handlers.instances.*;
import net.creeperhost.creeperlauncher.api.handlers.irc.*;
import net.creeperhost.creeperlauncher.api.handlers.other.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class WebSocketMessengerHandler
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static HashMap<TypeToken<? extends BaseData>, IMessageHandler<? extends BaseData>> handlers = new HashMap<TypeToken<? extends BaseData>, IMessageHandler<? extends BaseData>>();
    private static HashMap<String, Class<? extends BaseData>> dataMap = new HashMap<>();
    static Gson gson = new Gson();

    static
    {
        registerDataMap("installedInstances", InstalledInstancesData.class);
        registerHandler(InstalledInstancesData.class, new InstalledInstancesHandler());
        registerDataMap("launchInstance", LaunchInstanceData.class);
        registerHandler(LaunchInstanceData.class, new LaunchInstanceHandler());
        registerDataMap("instanceInfo", InstanceInfoData.class);
        registerHandler(InstanceInfoData.class, new InstanceInfoHandler());
        registerDataMap("installInstance", InstallInstanceData.class);
        registerHandler(InstallInstanceData.class, new InstallInstanceHandler());
        registerDataMap("cancelInstallInstance", CancelInstallInstanceData.class);
        registerHandler(CancelInstallInstanceData.class, new CancelInstallInstanceHandler());
        registerDataMap("updateInstance", UpdateInstanceData.class);
        registerHandler(UpdateInstanceData.class, new UpdateInstanceHandler());
        registerDataMap("uninstallInstance", UninstallInstanceData.class);
        registerHandler(UninstallInstanceData.class, new UninstallInstanceHandler());
        registerDataMap("instanceConfigure", InstanceConfigureData.class);
        registerHandler(InstanceConfigureData.class, new InstanceConfigureHandler());
        registerDataMap("instanceBrowse", BrowseInstanceData.class);
        registerHandler(BrowseInstanceData.class, new BrowseInstanceHandler());
        registerDataMap("getSettings", SettingsInfoData.class);
        registerHandler(SettingsInfoData.class, new SettingsInfoHandler());
        registerDataMap("saveSettings", SettingsConfigureData.class);
        registerHandler(SettingsConfigureData.class, new SettingsConfigureHandler());
        registerDataMap("modalCallback", OpenModalData.ModalCallbackData.class);
        registerHandler(OpenModalData.ModalCallbackData.class, new ModalCallbackHandler());
        registerDataMap("fileHash", FileHashData.class);
        registerHandler(FileHashData.class, new FileHashHandler());
        registerDataMap("storeAuthDetails", StoreAuthDetailsData.class);
        registerHandler(StoreAuthDetailsData.class, new StoreAuthDetailsHandler());
        registerDataMap("syncInstance", SyncInstanceData.class);
        registerHandler(SyncInstanceData.class, new SyncInstanceHandler());
        registerDataMap("ircConnect", IRCConnectData.class);
        registerHandler(IRCConnectData.class, new IRCConnectHandler());
        registerDataMap("ircSendMessage", IRCSendMessageData.class);
        registerHandler(IRCSendMessageData.class, new IRCSendMessageHandler());
        registerDataMap("ircQuitRequest", IRCQuitRequestData.class);
        registerHandler(IRCQuitRequestData.class, new IRCQuitRequestHandler());
        registerDataMap("uploadLogs", UploadLogsData.class);
        registerHandler(UploadLogsData.class, new UploadLogsHandler());
        registerDataMap("getJavas", GetJavasData.class);
        registerHandler(GetJavasData.class, new GetJavasHandler());
        registerDataMap("getFriends", GetFriendsData.class);
        registerHandler(GetFriendsData.class, new GetFriendsHandler());
        registerDataMap("blockFriend", BlockFriendData.class);
        registerHandler(BlockFriendData.class, new BlockFriendHandler());
        registerDataMap("addFriend", AddFriendData.class);
        registerHandler(AddFriendData.class, new AddFriendHandler());
        registerDataMap("instanceMods", InstanceModsData.class);
        registerHandler(InstanceModsData.class, new InstanceModsHandler());
        registerDataMap("yeetLauncher", YeetLauncherData.class);
        registerHandler(YeetLauncherData.class, new YeetLauncherHandler());
        registerDataMap("pong", PongLauncherData.class);
        registerHandler(PongLauncherData.class, new PongLauncherHandler());
        registerDataMap("ping", PingLauncherData.class);
        registerDataMap("messageClient", MessageClientData.class);
        registerHandler(MessageClientData.class, new MessageClientHandler());
        registerDataMap("shareInstance", ShareInstanceData.class);
        registerHandler(ShareInstanceData.class, new ShareInstanceHandler());
        registerDataMap("instanceInstallMod", InstanceInstallModData.class);
        registerHandler(InstanceInstallModData.class, new InstanceInstallModHandler());
    }

    public static void registerHandler(Class<? extends BaseData> clazz, IMessageHandler<? extends BaseData> handler)
    {
        TypeToken<? extends BaseData> typeToken = TypeToken.of(clazz);
        handlers.put(typeToken, handler);
    }

    public static void registerDataMap(String typeString, Class<? extends BaseData> clazz)
    {
        dataMap.put(typeString, clazz);
    }

    public static void handleMessage(String data)
    {
        JsonParser parser = new JsonParser();
        JsonElement parse = parser.parse(data);
        if (parse.isJsonObject())
        {
            JsonObject jsonObject = parse.getAsJsonObject();
            if (jsonObject.has("type"))
            {
                String type = jsonObject.get("type").getAsString();
                Class<? extends BaseData> dataType = dataMap.get(type);
                TypeToken typeToken = TypeToken.of(dataType);
                IMessageHandler<? extends BaseData> iMessageHandler = handlers.get(typeToken);
                if (iMessageHandler != null)
                {
                    try {
                        BaseData parsedData = gson.fromJson(data, typeToken.getType());
                        if (CreeperLauncher.isDevMode || (parsedData.secret != null && parsedData.secret.equals(CreeperLauncher.websocketSecret))) {
                            CompletableFuture.runAsync(() -> iMessageHandler.handle(parsedData), CreeperLauncher.taskExeggutor).exceptionally((t) -> {
                                LOGGER.error("Error handling message", t);
                                return null;
                            });
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
