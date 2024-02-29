package net.creeperhost.creeperlauncher;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.CompletableFuture;

public class Analytics {
    public static void sendInstallRequest(long packID, long packVersion, byte packType) {
//        Request.Builder analytics = Constants.createModpacksEndpoint(false, packType, "/" + packID + "/" + packVersion + "/install");
//        lazyGetRequest(analytics);
    }

    public static void sendPlayRequest(long packID, long packVersion, byte packType) {
//        lazyGetRequest(Constants.createModpacksEndpoint(false, packType, "/" + packID + "/" + packVersion + "/play"));
    }

    /**
     * We do not care if this fails, so we gobble the exception.
     */
    private static void lazyGetRequest(Request.Builder builder) {
        CompletableFuture.runAsync(() -> {
            try {
                OkHttpClient okHttpClient = Constants.httpClient();
                okHttpClient.newCall(builder.build()).execute();
            } catch (Exception e) {
                // We don't care if this fails.
            }
        });
    }
}
