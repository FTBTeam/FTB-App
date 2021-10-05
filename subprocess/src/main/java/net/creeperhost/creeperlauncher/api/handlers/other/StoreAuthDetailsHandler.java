package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.StoreAuthDetailsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.minetogether.lib.MineTogether;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.minetogether.lib.vpn.MineTogetherConnect;

public class StoreAuthDetailsHandler implements IMessageHandler<StoreAuthDetailsData>
{
    @Override
    public void handle(StoreAuthDetailsData data)
    {
        Constants.SECRET = data.mpSecret;
        Constants.KEY = data.mpKey;
        Constants.MT_HASH = data.mtHash;
        Constants.S3_BUCKET = data.s3Bucket;
        Constants.S3_HOST = data.s3Host;
        Constants.S3_KEY = data.s3Key;
        Constants.S3_SECRET = data.s3Secret;
        MineTogether.init("modpacklauncher/" + Constants.APPVERSION + " Mozilla/5.0 (" + OS.CURRENT.name() + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56", Constants.KEY, Constants.SECRET, Settings.settings.getOrDefault("sessionString", ""));
        CreeperLauncher.mtConnect = new MineTogetherConnect(Constants.MT_HASH, () -> Settings.settings.getOrDefault("mtConnect", "false").equalsIgnoreCase("true"), Constants.MTCONNECT_DIR, Settings.settings.get("sessionString"));
        CloudSaveManager.setup(Constants.S3_HOST, 8080, Constants.S3_KEY, Constants.S3_SECRET, Constants.S3_BUCKET);
    }
}
