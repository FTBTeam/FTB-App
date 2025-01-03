package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.UploadLogsData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.util.LogsUploader;

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
