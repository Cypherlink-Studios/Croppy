package dev.darkblade.croppy.platform.paper.resourcepack;

import dev.darkblade.croppy.api.resourcepack.PackBuildResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedPackHttpServerTest {

    private EmbeddedPackHttpServer server;
    private int port;
    private Path packFile;
    private PackBuildResult buildResult;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        packFile = tempDir.resolve("croppy-unified-pack.zip");
        byte[] dummyZipBytes = "PK\u0003\u0004dummy-zip-content".getBytes(StandardCharsets.UTF_8);
        Files.write(packFile, dummyZipBytes);

        buildResult = new PackBuildResult(
                packFile,
                "0123456789abcdef0123456789abcdef01234567",
                new byte[20],
                dummyZipBytes.length,
                5,
                150L
        );

        server = new EmbeddedPackHttpServer(
                "127.0.0.1",
                port,
                packFile,
                () -> buildResult,
                Logger.getLogger(EmbeddedPackHttpServerTest.class.getName())
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testServePackOverHttp() throws Exception {
        assertTrue(server.start());
        assertTrue(server.isRunning());

        HttpClient client = HttpClient.newHttpClient();

        // 1. Test GET /pack/croppy-unified-pack.zip
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/pack/croppy-unified-pack.zip"))
                .GET()
                .build();

        HttpResponse<byte[]> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, getResponse.statusCode());
        assertEquals("application/zip", getResponse.headers().firstValue("Content-Type").orElse(""));
        assertEquals("\"0123456789abcdef0123456789abcdef01234567\"", getResponse.headers().firstValue("ETag").orElse(""));
        assertEquals("PK\u0003\u0004dummy-zip-content", new String(getResponse.body(), StandardCharsets.UTF_8));

        // 2. Test HEAD /pack/croppy-unified-pack.zip
        HttpRequest headRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/pack/croppy-unified-pack.zip"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> headResponse = client.send(headRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(200, headResponse.statusCode());
        assertEquals(String.valueOf(Files.size(packFile)), headResponse.headers().firstValue("Content-Length").orElse(""));

        // 3. Test 404 for unknown path
        HttpRequest notFoundRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/unknown"))
                .GET()
                .build();

        HttpResponse<Void> notFoundResponse = client.send(notFoundRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(404, notFoundResponse.statusCode());
    }

    @Test
    void testStopLifecycle() {
        assertTrue(server.start());
        assertTrue(server.isRunning());
        server.stop();
        org.junit.jupiter.api.Assertions.assertFalse(server.isRunning());
    }
}
