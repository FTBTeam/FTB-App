package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.data.other.StoreAuthDetailsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class StoreAuthDetailsHandler implements IMessageHandler<StoreAuthDetailsData> {

    @Override
    public void handle(StoreAuthDetailsData data) {
        Constants.SECRET = data.mpSecret;
        Constants.KEY = data.mpKey;
        CreeperLauncher.CLOUD_SAVE_MANAGER.configure(data.s3Host, data.s3Bucket, data.s3Key, data.s3Secret);
    }
}
