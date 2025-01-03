package dev.ftb.app.api.handlers.instances;

import com.google.gson.JsonSyntaxException;
import dev.ftb.app.Constants;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.CheckShareCodeData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.data.modpack.ShareManifest;

public class CheckShareCode implements IMessageHandler<CheckShareCodeData> {
    
    @Override
    public void handle(CheckShareCodeData data) {
        boolean success;
        try {
            ShareManifest manifest = ShareManifest.queryManifest(Constants.TRANSFER_HOST + data.shareCode + "/manifest.json");
            success = manifest != null;
        } catch (JsonSyntaxException e) {
            success = false;
        }
        WebSocketHandler.sendMessage(new CheckShareCodeData.Reply(data, success));
    }
}
