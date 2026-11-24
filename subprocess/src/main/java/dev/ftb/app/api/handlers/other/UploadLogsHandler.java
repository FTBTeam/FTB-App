package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.data.other.UploadLogsData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.util.LogZipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class UploadLogsHandler implements IMessageHandler<UploadLogsData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadLogsHandler.class);
    
    @Override
    public void handle(MessageContext context, UploadLogsData data) {
        var requestId = data.requestId;

        try {
            Path path = new LogZipper(context.services())
                .generate();
            
            context.reply(new UploadLogsData.Reply(requestId, path));
        } catch (Exception e) {
            context.reply(new UploadLogsData.Reply(requestId)); // error
            LOGGER.error("Failed to upload logs", e);
        }
    }
}
