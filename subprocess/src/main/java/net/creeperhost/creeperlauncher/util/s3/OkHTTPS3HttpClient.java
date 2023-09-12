package net.creeperhost.creeperlauncher.util.s3;

import com.google.common.collect.ImmutableList;
import net.covers1624.quack.io.IOUtils;
import okhttp3.*;
import okio.BufferedSink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

/**
 * Super basic implementation of S3's {@link SdkHttpClient} abstraction to use OkHTTP.
 * <p>
 * This is likely very horrible, and not overly configurable.
 * <p>
 * Created by covers1624 on 22/3/23.
 */
public class OkHTTPS3HttpClient implements SdkHttpClient {

    private static final List<String> IGNORE_HEADERS = ImmutableList.of("Host", "Content-Length", "Content-Type");

    private final Supplier<OkHttpClient> clientGetter;

    public OkHTTPS3HttpClient(Supplier<OkHttpClient> clientGetter) {
        this.clientGetter = clientGetter;
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        MetricCollector metricCollector = request.metricCollector().orElseGet(NoOpMetricCollector::create);
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, clientName());
        Request okHttpRequest = toOkHttpRequest(request);

        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() throws IOException {
                @SuppressWarnings ("resource") // Closing the body if it exists is sufficient.
                Response okHttpResponse = clientGetter.get().newCall(okHttpRequest).execute();
                SdkHttpResponse.Builder responseBuilder = SdkHttpResponse.builder()
                        .statusCode(okHttpResponse.code())
                        .statusText(okHttpResponse.message());

                okHttpResponse.headers().forEach(e -> {
                    responseBuilder.appendHeader(e.getFirst(), e.getSecond());
                });

                ResponseBody body = okHttpResponse.body();

                AbortableInputStream responseStream = body != null ? AbortableInputStream.create(body.byteStream(), body::close) : null;
                return HttpExecuteResponse.builder().response(responseBuilder.build()).responseBody(responseStream).build();
            }

            @Override
            public void abort() {
            }
        };
    }

    @Override
    public String clientName() {
        return "FTBApp - OkHTTP";
    }

    @Override
    public void close() {

    }

    private static Request toOkHttpRequest(HttpExecuteRequest request) {
        SdkHttpRequest httpRequest = request.httpRequest();
        Request.Builder builder = new Request.Builder();
        builder.url(HttpUrl.get(httpRequest.getUri()));
        switch (httpRequest.method()) {
            case HEAD -> builder.head();
            case GET -> builder.get();
            case DELETE -> builder.delete();
            case OPTIONS -> builder.method("OPTIONS", null);
            case PATCH -> builder.patch(new S3RequestBody(request));
            case POST -> builder.post(new S3RequestBody(request));
            case PUT -> builder.put(new S3RequestBody(request));
        }

        httpRequest.forEachHeader((name, value) -> {
            if (IGNORE_HEADERS.contains(name)) return;
            for (String val : value) {
                builder.addHeader(name, val);
            }
        });
        return builder.build();
    }

    // Mostly implemented based off the Apache implementation.
    private static final class S3RequestBody extends RequestBody {

        private static final Logger LOGGER = LogManager.getLogger();

        private final InputStream content;
        private final RequestBody delegate;

        private boolean isFirstWrite = true;
        @Nullable
        private IOException firstEx;

        private S3RequestBody(HttpExecuteRequest request) {
            long len = request.httpRequest().firstMatchingHeader("Content-Length")
                    .map(S3RequestBody::parseContentLength)
                    .orElse(-1L);
            MediaType contentType = request.httpRequest().firstMatchingHeader("Content-Type")
                    .map(MediaType::parse)
                    .orElse(null);

            content = request.contentStreamProvider().map(ContentStreamProvider::newStream)
                    .orElseGet(() -> new ByteArrayInputStream(new byte[0]));

            // We have to compute it, otherwise S3 gets mad.
            if (len == -1) {
                delegate = new RequestBody() {
                    private byte @Nullable [] bytes;

                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return contentType;
                    }

                    @Override
                    public long contentLength() throws IOException {
                        return getBytes().length;
                    }

                    @Override
                    public void writeTo(BufferedSink bufferedSink) throws IOException {
                        bufferedSink.write(getBytes());
                    }

                    private byte[] getBytes() throws IOException {
                        if (bytes == null) {
                            bytes = IOUtils.toBytes(content);
                        }
                        return bytes;
                    }
                };
            } else {
                delegate = new RequestBody() {
                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return contentType;
                    }

                    @Override
                    public long contentLength() throws IOException {
                        return len;
                    }

                    @Override
                    public void writeTo(BufferedSink bufferedSink) throws IOException {
                        int len;
                        byte[] bytes = IOUtils.getCachedBuffer();
                        while ((len = content.read(bytes)) != -1) {
                            bufferedSink.write(bytes, 0, len);
                        }
                    }

                    @Override
                    public boolean isOneShot() {
                        return !content.markSupported();
                    }
                };
            }
        }

        @Nullable
        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return delegate.contentLength();
        }

        @Override
        public void writeTo(BufferedSink bufferedSink) throws IOException {
            try {
                if (!isFirstWrite && !isOneShot()) {
                    content.reset();
                }
                isFirstWrite = false;
                delegate.writeTo(bufferedSink);
            } catch (IOException ex) {
                if (firstEx == null) {
                    firstEx = ex;
                } else {
                    firstEx.addSuppressed(ex);
                }
                throw firstEx;
            }
        }

        @Override
        public boolean isOneShot() {
            return !content.markSupported() && !delegate.isOneShot();
        }

        private static long parseContentLength(String contentLength) {
            try {
                return Long.parseLong(contentLength);
            } catch (NumberFormatException nfe) {
                LOGGER.warn("Unable to parse content length from request. Buffering contents in memory. " + contentLength);
                return -1;
            }
        }
    }
}
