package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.Hashing;
import fi.iki.elonen.NanoHTTPD;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.covers1624.quack.util.SneakyUtils;
import net.covers1624.quack.util.TimeUtils;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 14/4/22.
 */
public class NewDownloadTaskTests {

    static {
        System.setProperty("DownloadTask.debug", "true");
    }

    @Test
    public void testCompanionHashes() throws Throwable {
        Path tempDir = Files.createTempDirectory("dlTask");
        tempDir.toFile().deleteOnExit();

        NewDownloadTask task = NewDownloadTask.builder()
                .url("https://repo1.maven.org/maven2/com/google/guava/guava/maven-metadata.xml")
                .dest(tempDir.resolve("maven-metadata.xml"))
                .tryCompanionHashes()
                .build();
        task.execute(null, null);
        assertNotNull(task.getValidation().expectedHashes.get(HashFunc.SHA1));
    }

    @Test
    public void testEtagChange() throws Throwable {
        try (TestWebServer testWebServer = new TestWebServer()) {
            Path dest = Files.createTempFile("tmp", ".dat");
            dest.toFile().deleteOnExit();
            Files.delete(dest);

            String path = "/test/etag_change";
            NewDownloadTask firstRequest = NewDownloadTask.builder()
                    .url(testWebServer.getAddr() + path)
                    .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                    .dest(dest)
                    .build();
            firstRequest.execute(null, null);
            BakedResponse resp = testWebServer.responseMap.get(path);

            assertArrayEquals(resp.bytes, Files.readAllBytes(dest));
            assertEquals(resp.etag, Files.readString(dest.resolveSibling(dest.getFileName() + ".etag")));
            assertEquals(resp.lastModified / 1000, Files.getLastModifiedTime(dest).toMillis() / 1000);

            resp = testWebServer.generateResponseData(resp);
            testWebServer.responseMap.put(path, resp);

            NewDownloadTask secondRequest = NewDownloadTask.builder()
                    .url(testWebServer.getAddr() + path)
                    .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                    .dest(dest)
                    .build();
            secondRequest.execute(null, null);

            assertArrayEquals(resp.bytes, Files.readAllBytes(dest));
            assertEquals(resp.etag, Files.readString(dest.resolveSibling(dest.getFileName() + ".etag")));
            assertEquals(resp.lastModified / 1000, Files.getLastModifiedTime(dest).toMillis() / 1000);
        }
    }

    @Test
    public void testMirrors() throws IOException {
        try (TestWebServer testWebServer = new TestWebServer()) {
            Path dest = Files.createTempFile("tmp", ".dat");
            dest.toFile().deleteOnExit();
            Files.delete(dest);

            String pathA = "/test/missing";
            testWebServer.responseMap.put(pathA, new BakedResponse(null, "", -1));
            String pathB = "/test/notMissing";

            NewDownloadTask firstRequest = NewDownloadTask.builder()
                    .url(testWebServer.getAddr() + pathA)
                    .withMirror(testWebServer.getAddr() + pathB)
                    .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                    .dest(dest)
                    .build();
            firstRequest.execute(null, null);
        }
    }

    @Test
    public void testMirrorsFail() throws IOException {
        try (TestWebServer testWebServer = new TestWebServer()) {
            Path dest = Files.createTempFile("tmp", ".dat");
            dest.toFile().deleteOnExit();
            Files.delete(dest);

            String pathA = "/test/missing";
            String pathB = "/test/missing2";
            testWebServer.responseMap.put(pathA, new BakedResponse(null, "", -1));
            testWebServer.responseMap.put(pathB, new BakedResponse(null, "", -1));

            Assertions.assertThrows(DownloadFailedException.class, () -> {
                NewDownloadTask firstRequest = NewDownloadTask.builder()
                        .url(testWebServer.getAddr() + pathA)
                        .withMirror(testWebServer.getAddr() + pathB)
                        .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                        .dest(dest)
                        .build();
                firstRequest.execute(null, null);
            });
        }
    }

    @SuppressWarnings ("UnstableApiUsage")
    private static class TestWebServer extends NanoHTTPD implements AutoCloseable {

        private static final Random random = new Random();

        public final Map<String, BakedResponse> responseMap = new HashMap<>();

        private final int port;

        public TestWebServer() throws IOException {
            this(MiscUtils.getRandomEphemeralPort());
        }

        public TestWebServer(int port) throws IOException {
            super(port);
            this.port = port;
            start();
        }

        public String getAddr() {
            return "http://localhost:" + port;
        }

        @Override
        public void close() {
            stop();
        }

        @Override
        public Response serve(IHTTPSession session) {
            return SneakyUtils.sneaky(() -> serveInternal(session));
        }

        private Response serveInternal(IHTTPSession session) throws Throwable {
            BakedResponse response = responseMap.get(session.getUri());
            if (response != null) {
                // Null bytes simulates a failure of some kind.
                // Just return bad request, 404 triggers special handling.
                if (response.bytes == null) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, null, null);
                }
                String etagHeader = session.getHeaders().get("if-none-match");
                Date modifiedHeader = TimeUtils.parseDate(session.getHeaders().get("if-modified-since"));
                if (response.etag.equals(etagHeader) && (modifiedHeader == null || Objects.equals(modifiedHeader.getTime(), response.lastModified))) {
                    return newFixedLengthResponse(Response.Status.NOT_MODIFIED, null, null);
                }
            } else {
                byte[] data = genRandomData();
                response = new BakedResponse(
                        data,
                        HashUtils.hash(Hashing.sha256(), new ByteArrayInputStream(data)).toString(),
                        System.currentTimeMillis()
                );
                responseMap.put(session.getUri(), response);
            }
            Response resp = newFixedLengthResponse(Response.Status.OK, null, new ByteArrayInputStream(response.bytes), response.bytes.length);
            resp.addHeader("ETag", response.etag);
            resp.addHeader("Last-Modified", TimeUtils.FORMAT_RFC1123.format(new Date(response.lastModified)));
            return resp;
        }

        public BakedResponse generateResponseData(BakedResponse prev) throws IOException {
            byte[] data = genRandomData();
            return new BakedResponse(
                    data,
                    HashUtils.hash(Hashing.sha256(), new ByteArrayInputStream(data)).toString(),
                    prev.lastModified + random.nextInt(30000)
            );
        }

        private static byte[] genRandomData() {
            int len = random.nextInt(1024 * 4);
            byte[] bytes = new byte[len];
            random.nextBytes(bytes);
            return bytes;
        }
    }

    private record BakedResponse(@Nullable byte[] bytes, String etag, long lastModified) { }
}
