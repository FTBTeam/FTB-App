package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.other.UploadLogsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.LogsUploader;

public class UploadLogsHandler implements IMessageHandler<UploadLogsData> {

    @Override
    public void handle(UploadLogsData data) {
        uploadLogs(data.requestId);
    }

    public static void uploadLogs(String requestId) {
        String code = LogsUploader.uploadUILogs();
        if (code == null) {
            WebSocketHandler.sendMessage(new UploadLogsData.Reply(requestId)); // error
            return;
        }
        WebSocketHandler.sendMessage(new UploadLogsData.Reply(requestId, code));
    }
}
