package dev.ftb.app.api.handlers.other;

import dev.ftb.app.AppMain;
import dev.ftb.app.Constants;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.install.tasks.DownloadTask;

import java.nio.file.Path;

public class VideoCacheHandler implements IMessageHandler<VideoCacheHandler.Data> {
    @Override
    public void handle(Data data) {
        Path dest = Constants.BIN_LOCATION.resolve("media/" + data.fileName);
        DownloadTask task = DownloadTask.builder()
            .url(data.url)
            .dest(dest)
            .withFileLocator(AppMain.localCache)
            .build();
        
        if (task.isRedundant()) {
            WebSocketHandler.sendMessage(new VideoCacheHandler.Reply(data, dest.toString()));
            return;
        }
        
        try {
            task.execute(null, null);
            WebSocketHandler.sendMessage(new VideoCacheHandler.Reply(data, dest.toString()));
        } catch (Throwable e) {
            // If it fails for any reason, just return the url
            WebSocketHandler.sendMessage(new VideoCacheHandler.Reply(data, data.url));
        }
    }

    public static class Data extends BaseData {
        public String url;
        public String fileName;
    }
    
    public static class Reply extends Data {
        public String location;
        
        public Reply(Data data, String location) { 
            this.type = "videoCacheReply";
            this.requestId = data.requestId;
            this.url = data.url;
            this.location = location;
        }
    }
}
