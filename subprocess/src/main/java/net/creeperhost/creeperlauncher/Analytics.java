package net.creeperhost.creeperlauncher;

import net.creeperhost.creeperlauncher.util.WebUtils;
import java.util.concurrent.CompletableFuture;

public class Analytics
{
    public static void sendInstallRequest(long packID, long packVersion, byte packType)
    {
        String analytics = Constants.getCreeperhostModpackSearch2(false, packType) + "/" + packID + "/" + packVersion + "/install";
        CompletableFuture.runAsync(() -> {
            WebUtils.getAPIResponse(analytics);
        });
    }

    public static void sendPlayRequest(long packID, long packVersion, byte packType)
    {
        String analytics = Constants.getCreeperhostModpackSearch2(false, packType) + "/" + packID + "/" + packVersion + "/play";
        CompletableFuture.runAsync(() -> {
            WebUtils.getAPIResponse(analytics);
        });
    }
}
