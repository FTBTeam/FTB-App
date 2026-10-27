package dev.ftb.app;

import dev.ftb.app.services.AppHttp;
import dev.ftb.app.util.ModpackApiUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.CompletableFuture;

public class Analytics {
    public static void sendInstallRequest(AppHttp http, long packID, long packVersion, byte packType) {
        lazyGetRequest(http, ModpackApiUtils.getModpacksEndpoint(false, packType) + "/" + packID + "/" + packVersion + "/install");
    }

    public static void sendPlayRequest(AppHttp http, long packID, long packVersion, byte packType) {
        lazyGetRequest(http, ModpackApiUtils.getModpacksEndpoint(false, packType) + "/" + packID + "/" + packVersion + "/play");
    }

    /**
     * We do not care if this fails, so we gobble the exception.
     */
    private static void lazyGetRequest(AppHttp http, String endpoint) {
        CompletableFuture.runAsync(() -> {
            try {
                OkHttpClient okHttpClient = http.httpClient();
                okHttpClient.newCall(new Request.Builder().url(endpoint).build()).execute().close();
            } catch (Exception e) {
                // We don't care if this fails.
            }
        });
    }
}
