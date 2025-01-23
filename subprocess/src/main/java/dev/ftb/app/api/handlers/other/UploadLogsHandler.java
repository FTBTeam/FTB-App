package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.UploadLogsData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.util.LogZipper;

import java.nio.file.Path;

public class UploadLogsHandler implements IMessageHandler<UploadLogsData> {

    @Override
    public void handle(UploadLogsData data) {
        var requestId = data.requestId;

        try {
            Path path = LogZipper.create().generate();
            WebSocketHandler.sendMessage(new UploadLogsData.Reply(requestId, path));
        } catch (Exception e) {
            WebSocketHandler.sendMessage(new UploadLogsData.Reply(requestId)); // error
        }
    }
}
