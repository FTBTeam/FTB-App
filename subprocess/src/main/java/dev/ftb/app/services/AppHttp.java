package dev.ftb.app.services;

import com.google.common.base.Suppliers;
import dev.ftb.app.AppFlags;
import dev.ftb.app.Constants;
import dev.ftb.app.os.OS;
import dev.ftb.app.util.FetchLoggerInterceptor;
import dev.ftb.app.util.ProxyUtils;
import dev.ftb.app.util.SSLUtils;
import dev.ftb.app.util.SimpleCookieJar;
import net.covers1624.quack.net.okhttp.MultiHasherInterceptor;
import net.covers1624.quack.net.okhttp.ThrottlerInterceptor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okio.Throttler;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class AppHttp {
    private final Throttler clientThrottler = new Throttler();
    private final Supplier<String> userAgent;

    private OkHttpClient httpClientV2;
    private OkHttpClient httpClientV1;
    
    private final AppFlags flags;
    
    private final LongSupplier speedLimit;
    
    public AppHttp(AppFlags flags, LongSupplier speedLimit) {
        this.flags = flags;
        this.speedLimit = speedLimit;

        // This needs to happen before the client is created.
        this.userAgent = Suppliers.memoize(() -> "ftb-app/" + Constants.APPVERSION + " " +
            "OS/" + OS.CURRENT.name() + " " +
            "Platform/" + this.flags.platform() + " " +
            "Branch/" + Constants.BRANCH + " " +
            "Commit/" + Constants.COMMIT);
        
        this.createClients();
    }

    public void createClients() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .connectionPool(new ConnectionPool())
            .cookieJar(new SimpleCookieJar())
            .addInterceptor(new FetchLoggerInterceptor())
            .addInterceptor(new MultiHasherInterceptor())
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().tag(Throttler.class, getClientThrottler()).build()))
            .addInterceptor(new ThrottlerInterceptor())
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", this.userAgent.get()).build()));

        SSLUtils.inject(builder);
        ProxyUtils.inject(builder);
        this.httpClientV2 = builder.build();
        builder.protocols(List.of(Protocol.HTTP_1_1));
        this.httpClientV1 = builder.build();
    }

    private Throttler getClientThrottler() {
        // TODO, double-check this logic.
        long maxSpeed = this.speedLimit.getAsLong() * 1000L;
        if (maxSpeed > 0) {
            this.clientThrottler.bytesPerSecond((maxSpeed / 8) * 2); //Some reason it always limits to 50% for me, so we multiply!
        } else {
            this.clientThrottler.bytesPerSecond(Long.MAX_VALUE);
        }

        return this.clientThrottler;
    }
    
    public OkHttpClient httpClient() {
        return this.httpClient(Protocol.HTTP_2);
    }
    
    public OkHttpClient httpClient(Protocol protocol) {
        return switch (protocol) {
            case HTTP_1_1 -> this.httpClientV1;
            case HTTP_2 -> this.httpClientV2;
            default -> throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        };
    }

    public Supplier<String> userAgent() {
        return userAgent;
    }
}
