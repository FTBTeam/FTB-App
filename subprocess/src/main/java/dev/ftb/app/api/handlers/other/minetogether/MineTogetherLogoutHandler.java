package dev.ftb.app.api.handlers.other.minetogether;

import dev.ftb.app.AppMain;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.CredentialStorage;
import dev.ftb.app.util.ModpacksChUtils;

public class MineTogetherLogoutHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        // Yeet the credential store
        CredentialStorage.getInstance().remove("minetogether");
        CredentialStorage.getInstance().remove("modpacksChApiKey");

        System.out.println(CredentialStorage.getInstance().getCredentials());

        // This will close the save manager
        AppMain.CLOUD_SAVE_MANAGER.configure(new S3Credentials("", "", "", ""));
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
