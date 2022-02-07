package net.creeperhost.creeperlauncher.install.tasks.http;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import net.covers1624.quack.util.SneakyUtils;
import net.creeperhost.creeperlauncher.util.SimpleCookieJar;
import okhttp3.*;
import okio.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * Http client based on {@link OkHttpClient}.
 */
@Deprecated
@SuppressWarnings ("UnstableApiUsage")
public class OkHttpClientImpl implements IHttpClient
{
    private static final OkHttpClient client;

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cookieJar(new SimpleCookieJar());
        builder.addInterceptor((chain) ->
        {
            Request request = chain.request();
            Response originalResponse = chain.proceed(request);
            ResponseBody body = originalResponse.body();
            if (body == null) return originalResponse;
            return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(body, requireNonNull(request.tag(ResponseHandlers.class)), requireNonNull(request.tag(Long.class))))
                    .build();
        });
        client = builder.build();
    }

    @Override
    public DownloadedFile doDownload(String url, Path destination, @Nullable IProgressUpdater progressWatcher, @Nullable HashFunction hashFunc, long maxSpeed) throws IOException
    {
        Hasher hasher = hashFunc != null ? hashFunc.newHasher() : null;
        ResponseHandlers responseHandlers = new ResponseHandlers(progressWatcher, e -> {
            if (hasher != null) {
                hasher.putBytes(e.readByteArray());
            }
        });

        Request request = new Request.Builder()
                .url(url)
                .tag(Long.class, maxSpeed)
                .tag(ResponseHandlers.class, responseHandlers)
                .build();

        try (Response response = client.newCall(request).execute()) {
            try (BufferedSink sink = Okio.buffer(Okio.sink(destination))) {
                ResponseBody body = response.body();
                if (body == null) throw new FileNotFoundException("Response had no body.");
                sink.writeAll(body.source());
            }
        }

        return new DownloadedFile(destination, Files.size(destination), hasher != null ? hasher.hash() : null);
    }

    private static class ProgressResponseBody extends ResponseBody
    {

        private final Throttler speed = new Throttler();
        private final ResponseBody responseBody;
        private final ResponseHandlers responseHandlers;
        private final long maxSpeed;
        @Nullable
        private BufferedSource bufferedSource;

        public ProgressResponseBody(ResponseBody responseBody, ResponseHandlers responseHandlers, long maxSpeed)
        {
            this.responseBody = responseBody;
            this.responseHandlers = responseHandlers;
            this.maxSpeed = maxSpeed;
        }

        @Override
        public MediaType contentType()
        {
            return responseBody.contentType();
        }

        @Override
        public long contentLength()
        {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source()
        {
            if (bufferedSource == null)
            {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source)
        {
            if (maxSpeed > 0)
            {
                speed.bytesPerSecond((maxSpeed / 8) * 2);//Some reason it always limits to 50% for me, so we multiply!
            } else
            {
                speed.bytesPerSecond(Long.MAX_VALUE);
            }
            return new ForwardingSource(speed.source(source))
            {
                long totalBytesRead = 0L;

                @Override
                public long read(@NotNull Buffer sink, long byteCount) throws IOException
                {
                    long bytesRead = super.read(sink, byteCount);
                    responseHandlers.handler.accept(sink.peek());

                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;

                    long totalBytes = responseBody.contentLength();

                    if (responseHandlers.updater != null)
                        responseHandlers.updater.update(totalBytesRead, bytesRead, totalBytes, totalBytes == totalBytesRead);

                    return bytesRead;
                }
            };
        }
    }

    private record ResponseHandlers(@Nullable IProgressUpdater updater, SneakyUtils.ThrowingConsumer<BufferedSource, IOException> handler) {
    }
}
