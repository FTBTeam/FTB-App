package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.StoreAuthDetailsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.minetogether.lib.MineTogether;

public class StoreAuthDetailsHandler implements IMessageHandler<StoreAuthDetailsData> {

    @Override
    public void handle(StoreAuthDetailsData data) {
        Constants.SECRET = data.mpSecret;
        Constants.KEY = data.mpKey;
        Constants.MT_HASH = data.mtHash;
        MineTogether.init("modpacklauncher/" + Constants.APPVERSION + " Mozilla/5.0 (" + OS.CURRENT.name() + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56", Constants.KEY, Constants.SECRET, Settings.settings.getOrDefault("sessionString", ""));
        CreeperLauncher.CLOUD_SAVE_MANAGER.configure(data.s3Host, data.s3Bucket, data.s3Key, data.s3Secret);
    }
}
