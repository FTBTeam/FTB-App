package dev.ftb.app.services;

import dev.ftb.app.services.accounts.ApiTokenSource;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.function.Supplier;

/// Sends the signed in FTB account's access token with every request to the modpack API, and only to it.
public final class ModpackApiAuthInterceptor implements Interceptor {
    private final HttpUrl base;
    private final Supplier<ApiTokenSource> tokens;

    /// @param base   requests under this URL get the token
    /// @param tokens read for every request
    public ModpackApiAuthInterceptor(String base, Supplier<ApiTokenSource> tokens) {
        this.base = HttpUrl.get(base);
        this.tokens = tokens;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!this.isModpackApi(request.url())) {
            return chain.proceed(request);
        }

        ApiTokenSource source = this.tokens.get();
        String token = source.token();
        if (token == null) {
            return chain.proceed(request);
        }

        Response response = chain.proceed(withBearer(request, token));
        if (response.code() != 401 || (request.body() != null && request.body().isOneShot())) {
            return response;
        }

        String replacement = source.tokenAfterRejection(token);
        if (replacement == null || replacement.equals(token)) {
            return response;
        }

        response.close();
        return chain.proceed(withBearer(request, replacement));
    }

    boolean isModpackApi(HttpUrl url) {
        if (!url.scheme().equals(this.base.scheme()) || !url.host().equalsIgnoreCase(this.base.host()) || url.port() != this.base.port()) {
            return false;
        }

        String basePath = this.base.encodedPath();
        String path = url.encodedPath();
        return path.equals(basePath) || path.startsWith(basePath.endsWith("/") ? basePath : basePath + "/");
    }

    private static Request withBearer(Request request, String token) {
        return request.newBuilder().header("Authorization", "Bearer " + token).build();
    }
}
