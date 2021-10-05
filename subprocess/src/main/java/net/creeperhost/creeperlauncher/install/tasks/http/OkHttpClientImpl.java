package net.creeperhost.creeperlauncher.install.tasks.http;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import net.covers1624.quack.util.SneakyUtils;
import okhttp3.*;
import okio.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class OkHttpClientImpl implements IHttpClient
{
    private static final OkHttpClient client;

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor((chain) ->
        {
            Request request = chain.request();
            Response originalResponse = chain.proceed(request);
            return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), request.tag(ResponseHandlers.class), request.tag(Long.class)))
                    .build();
        });
        client = builder.build();
    }

    @Override
    public String makeRequest(String url)
    {
        return null;
    }

    /* returns when done */
    @Override
    public DownloadedFile doDownload(String url, Path destination, IProgressUpdater progressWatcher, HashFunction hashFunc, long maxSpeed) throws IOException
    {
        Hasher hasher = hashFunc.newHasher();
        ResponseHandlers responseHandlers = new ResponseHandlers(progressWatcher, e -> hasher.putBytes(e.readByteArray()));

        Request request = new Request.Builder()
                .url(url)
                .tag(Long.class, maxSpeed)
                .tag(ResponseHandlers.class, responseHandlers)
                .build();

        Response response = client.newCall(request).execute();

        BufferedSink sink = Okio.buffer(Okio.sink(destination));
        sink.writeAll(response.body().source());
        sink.close();

        response.close();

        return new DownloadedFile(destination, 0, hasher.hash());
    }

    private static class ProgressResponseBody extends ResponseBody
    {

        private final Throttler speed = new Throttler();
        private final ResponseBody responseBody;
        private final ResponseHandlers responseHandlers;
        private final long maxSpeed;
        private BufferedSource bufferedSource;

        public ProgressResponseBody(ResponseBody responseBody, ResponseHandlers progressListener, long maxSpeed)
        {
            this.responseBody = responseBody;
            this.responseHandlers = progressListener;
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

    private static class ResponseHandlers
    {
        private final IProgressUpdater updater;
        private final SneakyUtils.ThrowingConsumer<BufferedSource, IOException> handler;

        public ResponseHandlers(IProgressUpdater updater, SneakyUtils.ThrowingConsumer<BufferedSource, IOException> handler)
        {
            this.updater = updater;
            this.handler = handler;
        }
    }
}
