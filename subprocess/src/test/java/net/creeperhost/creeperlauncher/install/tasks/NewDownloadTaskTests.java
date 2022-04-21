package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
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
            Response resp = testWebServer.responseMap.get(path);

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

    @ChannelHandler.Sharable
    @SuppressWarnings ("UnstableApiUsage")
    private static class TestWebServer extends SimpleChannelInboundHandler<FullHttpRequest> implements AutoCloseable {

        private static final Random random = new Random();

        public final Map<String, Response> responseMap = new HashMap<>();

        private final int port;
        private final EventLoopGroup group;
        private final Channel channel;

        public TestWebServer() {
            this(MiscUtils.getRandomEphemeralPort());
        }

        public TestWebServer(int port) {
            this.port = port;
            try {
                group = new NioEventLoopGroup(0, new ThreadFactoryBuilder()
                        .setNameFormat("TestWebServer Netty IO #%d")
                        .setDaemon(true)
                        .build()
                );

                ServerBootstrap b = new ServerBootstrap();
                b.group(group).channel(NioServerSocketChannel.class);
                b.childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(@NotNull Channel ch) throws Exception {
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast(new HttpServerCodec());
                        pipe.addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                        pipe.addLast(new ChunkedWriteHandler());
                        pipe.addLast(TestWebServer.this);
                    }
                });
                channel = b.bind("localhost", port).sync().channel();
            } catch (InterruptedException ex) {
                throw new RuntimeException("Failed to start netty server.", ex);
            }
        }

        public String getAddr() {
            return "http://localhost:" + port;
        }

        @Override
        public void close() {
            channel.close().awaitUninterruptibly();
            group.shutdownGracefully();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            QueryStringDecoder decoder = new QueryStringDecoder(msg.uri());

            Response response = responseMap.get(decoder.path());
            if (response != null) {
                String etagHeader = msg.headers().get(HttpHeaderNames.IF_NONE_MATCH);
                Long modifiedHeader = msg.headers().getTimeMillis(HttpHeaderNames.IF_MODIFIED_SINCE);
                if (response.etag.equals(etagHeader) && Objects.equals(modifiedHeader, response.lastModified)) {
                    ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED))
                            .addListener(ChannelFutureListener.CLOSE);
                    return;
                }
            } else {
                byte[] data = genRandomData();
                response = new Response(
                        data,
                        HashUtils.hash(Hashing.sha256(), new ByteArrayInputStream(data)).toString(),
                        System.currentTimeMillis()
                );
                responseMap.put(decoder.path(), response);
            }
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(response.bytes)
            );
            resp.headers().add(HttpHeaderNames.ETAG, response.etag);
            resp.headers().add(HttpHeaderNames.LAST_MODIFIED, DateFormatter.format(new Date(response.lastModified)));

            ctx.writeAndFlush(resp)
                    .addListener(ChannelFutureListener.CLOSE);
        }

        public Response generateResponseData(Response prev) throws IOException {
            byte[] data = genRandomData();
            return new Response(
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

    private record Response(byte[] bytes, String etag, long lastModified) { }
}
