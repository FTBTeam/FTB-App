package net.creeperhost.creeperlauncher;

import net.creeperhost.creeperlauncher.util.ModpacksChUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.CompletableFuture;

public class Analytics {
    public static void sendInstallRequest(long packID, long packVersion, byte packType) {
        lazyGetRequest(ModpacksChUtils.getModpacksEndpoint(false, packType) + "/" + packID + "/" + packVersion + "/install");
    }

    public static void sendPlayRequest(long packID, long packVersion, byte packType) {
        lazyGetRequest(ModpacksChUtils.getModpacksEndpoint(false, packType) + "/" + packID + "/" + packVersion + "/play");
    }

    /**
     * We do not care if this fails, so we gobble the exception.
     */
    private static void lazyGetRequest(String endpoint) {
        CompletableFuture.runAsync(() -> {
            try {
                OkHttpClient okHttpClient = Constants.httpClient();
                okHttpClient.newCall(new Request.Builder().url(endpoint).build()).execute().close();
            } catch (Exception e) {
                // We don't care if this fails.
            }
        });
    }
}
