package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.UploadLogsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.LogsUploader;

public class UploadLogsHandler implements IMessageHandler<UploadLogsData> {

    @Override
    public void handle(UploadLogsData data) {
        uploadLogs(data.uiVersion, data.requestId);
    }

    public static void uploadLogs(String uiVersion, String requestId) {
        String code = LogsUploader.uploadUILogs(uiVersion);
        if (code == null) {
            Settings.webSocketAPI.sendMessage(new UploadLogsData.Reply(requestId)); // error
        }
        Settings.webSocketAPI.sendMessage(new UploadLogsData.Reply(requestId, code));
    }
}
