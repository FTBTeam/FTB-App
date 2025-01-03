package dev.ftb.app.install.tasks;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import dev.ftb.app.install.tasks.DownloadTask.DownloadValidation;
import dev.ftb.app.install.tasks.NewDownloadTaskTests.EtagTestWebServer.BakedResponse;
import dev.ftb.app.util.MiscUtils;
import fi.iki.elonen.NanoHTTPD;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.covers1624.quack.util.SneakyUtils;
import net.covers1624.quack.util.TimeUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static fi.iki.elonen.NanoHTTPD.Response.Status.PARTIAL_CONTENT;
import static org.junit.jupiter.api.Assertions.*;

public class NewDownloadTaskTests {

    private static final Random random = new Random();

    static {
        System.setProperty("DownloadTask.debug", "true");
    }

    @Test
    public void testCompanionHashes() throws Throwable {
        Path tempDir = Files.createTempDirectory("dlTask");
        tempDir.toFile().deleteOnExit();

        DownloadTask task = DownloadTask.builder()
                .url("https://repo1.maven.org/maven2/com/google/guava/guava/maven-metadata.xml")
                .dest(tempDir.resolve("maven-metadata.xml"))
                .tryCompanionHashes()
                .build();
        task.execute(null, null);
        assertNotNull(task.getValidation().expectedHashes.get(HashFunc.SHA1));
    }

    @Test
    public void testEtagChange() throws Throwable {
        try (EtagTestWebServer testWebServer = new EtagTestWebServer()) {
            Path dest = Files.createTempFile("tmp", ".dat");
            dest.toFile().deleteOnExit();
            Files.delete(dest);

            String path = "/test/etag_change";
            DownloadTask firstRequest = DownloadTask.builder()
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

            DownloadTask secondRequest = DownloadTask.builder()
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
        try (EtagTestWebServer testWebServer = new EtagTestWebServer()) {
            Path dest = Files.createTempFile("tmp", ".dat");
            dest.toFile().deleteOnExit();
            Files.delete(dest);

            String pathA = "/test/missing";
            testWebServer.responseMap.put(pathA, new BakedResponse(null, "", -1));
            String pathB = "/test/notMissing";

            DownloadTask firstRequest = DownloadTask.builder()
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
        try (EtagTestWebServer testWebServer = new EtagTestWebServer()) {
            Path dest = Files.createTempFile("tmp", ".dat");
            dest.toFile().deleteOnExit();
            Files.delete(dest);

            String pathA = "/test/missing";
            String pathB = "/test/missing2";
            testWebServer.responseMap.put(pathA, new BakedResponse(null, "", -1));
            testWebServer.responseMap.put(pathB, new BakedResponse(null, "", -1));

            Assertions.assertThrows(DownloadFailedException.class, () -> {
                DownloadTask firstRequest = DownloadTask.builder()
                        .url(testWebServer.getAddr() + pathA)
                        .withMirror(testWebServer.getAddr() + pathB)
                        .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                        .dest(dest)
                        .build();
                firstRequest.execute(null, null);
            });
        }
    }

    @Test
    public void testDownloadResume() throws Throwable {
        try (ResumeTestWebServer server = new ResumeTestWebServer()) {
            Path dest = Files.createTempFile("tmp", ".dat");
            dest.toFile().deleteOnExit();
            Files.delete(dest);

            String path = "/test";
            ResumeTestWebServer.ExpectedResponse resp = server.prepareRequest(path);

            DownloadTask task = DownloadTask.builder()
                    .url(server.getAddr() + path)
                    .withValidation(DownloadValidation.of().withExpectedSize(resp.bytes.length).withHash(HashFunc.SHA256, resp.sha256))
                    .dest(dest)
                    .tryResumeDownload()
                    .build();
            task.execute(null, null);
        }
    }

    private static byte[] genRandomData(int len) {
        byte[] bytes = new byte[len];
        random.nextBytes(bytes);
        return bytes;
    }

    @SuppressWarnings ("UnstableApiUsage")
    static class EtagTestWebServer extends NanoHTTPD implements AutoCloseable {

        public final Map<String, BakedResponse> responseMap = new HashMap<>();

        private final int port;

        public EtagTestWebServer() throws IOException {
            this(MiscUtils.getRandomEphemeralPort());
        }

        public EtagTestWebServer(int port) throws IOException {
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
                byte[] data = genRandomData(random.nextInt(1024 * 4));
                response = new BakedResponse(
                        data,
                        HashUtils.hash(Hashing.sha256(), new ByteArrayInputStream(data)).toString(),
                        System.currentTimeMillis()
                );
                responseMap.put(session.getUri(), response);
            }
            Response resp = newFixedLengthResponse(OK, null, new ByteArrayInputStream(response.bytes), response.bytes.length);
            resp.addHeader("ETag", response.etag);
            resp.addHeader("Last-Modified", TimeUtils.FORMAT_RFC1123.format(new Date(response.lastModified)));
            return resp;
        }

        public BakedResponse generateResponseData(BakedResponse prev) throws IOException {
            byte[] data = genRandomData(random.nextInt(1024 * 4));
            return new BakedResponse(
                    data,
                    HashUtils.hash(Hashing.sha256(), new ByteArrayInputStream(data)).toString(),
                    prev.lastModified + random.nextInt(30000)
            );
        }

        record BakedResponse(@Nullable byte[] bytes, String etag, long lastModified) { }
    }

    static class ResumeTestWebServer extends NanoHTTPD implements AutoCloseable {

        private final Map<String, ExpectedResponse> responseMap = new HashMap<>();

        public ResumeTestWebServer() throws IOException {
            super(MiscUtils.getRandomEphemeralPort());
            start();
        }

        public String getAddr() {
            return "http://localhost:" + getListeningPort();
        }

        @Override
        public Response serve(IHTTPSession session) {
            return SneakyUtils.sneaky(() -> serveInternal(session));
        }

        private Response serveInternal(IHTTPSession session) throws Throwable {
            ExpectedResponse resp = responseMap.get(session.getUri());
            if (resp == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found.");
            }

            String range = session.getHeaders().get("range");
            if (range != null && (!range.startsWith("bytes=") || !range.endsWith("-"))) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Expected Range: bytes=\\d- in request.");
            }
            int offset = range != null ? Integer.parseInt(range.replace("bytes=", "").replace("-", "")) : 0;

            int contentLength = resp.bytes.length - offset;
            return new Response(range != null ? PARTIAL_CONTENT : OK, null, null, -1) {
                @Override
                protected void send(OutputStream outputStream) {
                    SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                    gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                    try {
                        if (getStatus() == null) {
                            throw new Error("sendResponse(): Status can't be null.");
                        }
                        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII)), false);
                        pw.append("HTTP/1.1 ").append(getStatus().getDescription()).append(" \r\n");
                        if (getHeader("date") == null) {
                            printHeader(pw, "Date", gmtFrmt.format(new Date()));
                        }
                        if (getHeader("connection") == null) {
                            printHeader(pw, "Connection", "close");
                        }
                        pw.print("Content-Length: " + contentLength + "\r\n");
                        pw.append("\r\n");
                        pw.flush();
                        // Send buffer in 1024 blocks, flush and simulate a pre-emptive socket closure.
                        outputStream.write(resp.bytes, offset, 1024);
                        outputStream.flush();
                        if (contentLength != 1024) {
                            outputStream.close();
                        }
                    } catch (IOException ex) {
                        SneakyUtils.throwUnchecked(ex);
                    }
                }
            };
        }

        @Override
        public void close() throws Exception {
            stop();
        }

        private ExpectedResponse prepareRequest(String path) {
            byte[] data = genRandomData(1024 * 8);
            HashCode sha256 = Hashing.sha256().hashBytes(data);
            ExpectedResponse response = new ExpectedResponse(data, sha256);
            responseMap.put(path, response);
            return response;
        }

        record ExpectedResponse(byte[] bytes, HashCode sha256) { }
    }
}
