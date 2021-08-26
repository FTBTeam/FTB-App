package net.creeperhost.creeperlauncher.install.tasks.http;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import net.creeperhost.creeperlauncher.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaHttpClientImpl implements IHttpClient
{
    @Override
    public String makeRequest(String url)
    {
        return null;
    }

    @Override
    public DownloadedFile doDownload(String url, Path destination, IProgressUpdater progressWatcher, HashFunction hashFunc, long maxSpeed) throws IOException, ExecutionException, InterruptedException
    {
        Hasher hasher = hashFunc.newHasher();
        HttpClient client = HttpClient.newBuilder().executor(Runnable::run).build(); // always create
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        long fileSize = WebUtils.getFileSize(new URL(url));

        PathBodyHandlerProgress pathBodyHandlerProgress = new PathBodyHandlerProgress(destination, progressWatcher, hasher, fileSize);

        Path send = client.sendAsync(request, pathBodyHandlerProgress).get().body(); // not really async - our client will run async things on same thread. bit of a hack, but async just froze.

        return new DownloadedFile(send, pathBodyHandlerProgress.wrapper.downloadedBytes.get(), hasher.hash());
    }

    private void pushProgress(long totalRead, long delta, long contentLength, boolean done, IProgressUpdater progressWatcher)
    {
        if (progressWatcher != null) progressWatcher.update(totalRead, delta, contentLength, done);
    }

    class PathBodyHandlerProgress implements HttpResponse.BodyHandler<Path>
    {
        private final HttpResponse.BodyHandler<Path> pathBodyHandler;
        private BodySubscriberWrapper wrapper;
        private final Hasher hasher;
        private final IProgressUpdater progressWatcher;
        private final long fileSize;

        PathBodyHandlerProgress(Path destination, IProgressUpdater progressWatcher, Hasher hasher, long fileSize)
        {
            this.hasher = hasher;
            this.progressWatcher = progressWatcher;
            this.fileSize = fileSize;
            pathBodyHandler = HttpResponse.BodyHandlers.ofFile(destination);
        }

        @Override
        public HttpResponse.BodySubscriber<Path> apply(HttpResponse.ResponseInfo responseInfo)
        {
            return wrapper = new BodySubscriberWrapper(pathBodyHandler.apply(responseInfo), progressWatcher, hasher, fileSize);
        }
    }

    class BodySubscriberWrapper implements HttpResponse.BodySubscriber<Path>
    {

        private final HttpResponse.BodySubscriber<Path> delegate;
        private final IProgressUpdater progressWatcher;
        private final Hasher hasher;
        public AtomicInteger downloadedBytes = new AtomicInteger();
        private long fileSize;

        BodySubscriberWrapper(HttpResponse.BodySubscriber<Path> delegate, IProgressUpdater progressWatcher, Hasher hasher, long fileSize)
        {
            this.fileSize = fileSize;
            this.hasher = hasher;
            this.delegate = delegate;
            this.progressWatcher = progressWatcher;
        }

        @Override
        public CompletionStage<Path> getBody()
        {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription)
        {
            delegate.onSubscribe(subscription);
            pushProgress(downloadedBytes.get(), 0, fileSize, false, progressWatcher);
        }

        @Override
        public void onNext(List<ByteBuffer> item)
        {
            for (ByteBuffer bb : item)
            {
                hasher.putBytes(bb);
                bb.rewind();
            }
            int sum = item.stream().mapToInt(Buffer::remaining).sum();
            pushProgress(downloadedBytes.get(), sum, fileSize, false, progressWatcher);
            delegate.onNext(item);
        }

        @Override
        public void onError(Throwable throwable)
        {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete()
        {
            delegate.onComplete();
            pushProgress(downloadedBytes.get(), 0, fileSize, true, progressWatcher);
        }
    }
}
