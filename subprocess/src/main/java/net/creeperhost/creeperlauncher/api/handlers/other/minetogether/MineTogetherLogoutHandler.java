package net.creeperhost.creeperlauncher.api.handlers.other.minetogether;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.storage.CredentialStorage;
import net.creeperhost.creeperlauncher.util.ModpacksChUtils;

public class MineTogetherLogoutHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        // Yeet the credential store
        CredentialStorage.getInstance().remove("minetogether");
        CredentialStorage.getInstance().remove("modpacksChApiKey");

        System.out.println(CredentialStorage.getInstance().getCredentials());

        // This will close the save manager
        CreeperLauncher.CLOUD_SAVE_MANAGER.configure(new S3Credentials("", "", "", ""));
        ModpacksChUtils.API_TOKEN = "";
        
        WebSocketHandler.sendMessage(new Reply(data, true));
    }
    
    public class Reply extends BaseData {
        public boolean success;
        
        public Reply(BaseData baseData, boolean success) {
            super();
            this.requestId = baseData.requestId;
            this.type = "minetogetherLogoutReply";
            this.success = success;
        }
    }
}
