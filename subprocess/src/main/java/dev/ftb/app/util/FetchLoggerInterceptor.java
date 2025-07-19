package dev.ftb.app.util;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * An OkHttp interceptor that logs HTTP requests and responses.
 * It filters out sensitive information and ignores certain headers. (at least it tries to)
 */
public class FetchLoggerInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger("httplogger"); // Special logger for its own location

    /**
     * We don't care about these headers in the logs and they are often not useful.
     */
    private static final Set<String> IGNORED_HEADERS = Set.of(
        "content-length", "cache-control", "user-agent", "report-to", "etag", "x-powered-by", "vary", "nel"
    );
    
    @Override
    public @NotNull Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        LOGGER.info("--------------------------------------------------");
        long startTime = System.currentTimeMillis();
        LOGGER.info("REQUEST:: {} ({})", request.url(), request.method());
        logHeader(request.headers());

        Response response = chain.proceed(request);

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("RESPONSE took {}ms with code: {} ({}) ",
                duration, response.code(), response.message());
        
        logHeader(response.headers());
        
        // Never log the body of a successful response, only log errors
        if (response.code() != 200) {
            // Peek the body and log it.
            String bodyString = response.peekBody(Long.MAX_VALUE).string();
            String filteredBody = LogZipper.JWT_REMOVAL.matcher(bodyString).replaceAll("REDACTED");
            LOGGER.error("Response body: {}", filteredBody);
        }

        return response;
    }
    
    private static void logHeader(Headers headers) {
        LOGGER.info("Headers:");
        if (headers.size() == 0) {
            LOGGER.info("-- No headers");
            return;
        }
        
        for (var header : headers) {
            var key = header.getFirst();
            if (IGNORED_HEADERS.contains(key.toLowerCase()) || key.startsWith("access-control-")) {
                continue; // Skip logging ignored headers
            }
            
            if (key.startsWith("Authorization") || key.startsWith("Cookie")) {
                // Redact sensitive information like Authorization tokens or Cookies
                LOGGER.info("-- {}: REDACTED", key);
                continue;
            }
            
            var filteredValue = LogZipper.JWT_REMOVAL.matcher(header.getSecond()).replaceAll("REDACTED");
            LOGGER.info("-- {}: {}", key, filteredValue);
        }
    }
}
