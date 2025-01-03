package dev.ftb.app.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 27/5/22.
 */
public class PathRequestBody extends RequestBody {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Path path;
    @Nullable
    private final MediaType mediaType;

    public PathRequestBody(Path path) {
        this(path, find(path));
    }

    public PathRequestBody(Path path, @Nullable MediaType mediaType) {
        this.path = path;
        this.mediaType = mediaType;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        return Files.size(path);
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        try (Source source = Okio.source(path)) {
            sink.writeAll(source);
        }
    }

    @Nullable
    private static MediaType find(Path path) {
        try {
            String probedType = Files.probeContentType(path);
            return probedType != null ? MediaType.parse(probedType) : null;
        } catch (IOException ex) {
            LOGGER.error("Failed to probe content type for {}.", path, ex);
            return null;
        }
    }
}
