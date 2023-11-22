package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;

import java.nio.file.Path;

public class VideoCacheHandler implements IMessageHandler<VideoCacheHandler.Data> {
    @Override
    public void handle(Data data) {
        Path dest = Constants.BIN_LOCATION.resolve("media/" + data.fileName);
        NewDownloadTask task = NewDownloadTask.builder()
            .url(data.url)
            .dest(dest)
            .withFileLocator(CreeperLauncher.localCache)
            .build();
        
        if (task.isRedundant()) {
            Settings.webSocketAPI.sendMessage(new VideoCacheHandler.Reply(data, dest.toString()));
            return;
        }
        
        try {
            task.execute(null, null);
            Settings.webSocketAPI.sendMessage(new VideoCacheHandler.Reply(data, dest.toString()));
        } catch (Throwable e) {
            // If it fails for any reason, just return the url
            Settings.webSocketAPI.sendMessage(new VideoCacheHandler.Reply(data, data.url));
        }
    }

    public static class Data extends BaseData {
        public String url;
        public String fileName;
    }
    
    public static class Reply extends Data {
        public String location;
        
        public Reply(Data data, String location) {
            this.requestId = data.requestId;
            this.url = data.url;
            this.location = location;
        }
    }
}
