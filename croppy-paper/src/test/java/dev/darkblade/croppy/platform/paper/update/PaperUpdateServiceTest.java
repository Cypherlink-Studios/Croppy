package dev.darkblade.croppy.platform.paper.update;

import com.sun.net.httpserver.HttpServer;
import dev.darkblade.croppy.api.update.SemanticVersion;
import dev.darkblade.croppy.api.update.UpdateApplyResult;
import dev.darkblade.croppy.api.update.UpdateChannel;
import dev.darkblade.croppy.api.update.UpdateCheckResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PaperUpdateServiceTest {

    private HttpServer server;
    private int port;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Should return disabled when update service is disabled in config")
    void testDisabledService(@TempDir Path tempDir) {
        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                "http://127.0.0.1:" + port,
                "Cypherlink-Studios/Croppy",
                SemanticVersion.of(0, 5, 13),
                Duration.ofMinutes(15)
        );

        PaperUpdateService service = new PaperUpdateService(
                client,
                tempDir,
                () -> "Croppy.jar",
                UpdateChannel.STABLE,
                false,
                Logger.getLogger("test")
        );

        UpdateCheckResult check = service.checkForUpdatesAsync(false).join();
        assertTrue(check.isFailed());
        assertTrue(check.errorMessage().orElseThrow().contains("disabled"));

        UpdateApplyResult apply = service.applyUpdateAsync().join();
        assertFalse(apply.success());
        assertTrue(apply.errorMessage().orElseThrow().contains("disabled"));
    }

    @Test
    @DisplayName("Should stage update binary when check succeeds and SHA-256 matches")
    void testApplyUpdateSuccess(@TempDir Path tempDir) {
        byte[] jarBytes = "valid-jar-binary-stream-0.5.14".getBytes(StandardCharsets.UTF_8);
        String correctHash = ChecksumValidator.computeSha256(jarBytes);

        String releaseJson = """
                {
                  "tag_name": "v0.5.14",
                  "html_url": "https://github.com/Cypherlink-Studios/Croppy/releases/tag/v0.5.14",
                  "published_at": "2026-11-08T12:00:00Z",
                  "assets": [
                    {
                      "name": "Croppy-0.5.14.jar",
                      "browser_download_url": "http://127.0.0.1:%d/download/Croppy-0.5.14.jar"
                    },
                    {
                      "name": "checksums-sha256.txt",
                      "browser_download_url": "http://127.0.0.1:%d/download/checksums-sha256.txt"
                    }
                  ]
                }
                """.formatted(port, port);

        String manifestContent = correctHash + "  Croppy-0.5.14.jar\n";

        server.createContext("/repos/Cypherlink-Studios/Croppy/releases/latest", exchange -> {
            byte[] b = releaseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        });

        server.createContext("/download/Croppy-0.5.14.jar", exchange -> {
            exchange.sendResponseHeaders(200, jarBytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(jarBytes); }
        });

        server.createContext("/download/checksums-sha256.txt", exchange -> {
            byte[] b = manifestContent.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        });

        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                "http://127.0.0.1:" + port,
                "Cypherlink-Studios/Croppy",
                SemanticVersion.of(0, 5, 13),
                Duration.ofMinutes(15)
        );

        Path updateDir = tempDir.resolve("update");
        PaperUpdateService service = new PaperUpdateService(
                client,
                updateDir,
                () -> "Croppy-0.5.13.jar",
                UpdateChannel.STABLE,
                true,
                Logger.getLogger("test")
        );

        UpdateApplyResult apply = service.applyUpdateAsync().join();
        assertTrue(apply.success());
        assertEquals("Croppy-0.5.13.jar", apply.stagedFileName().orElseThrow());
        assertEquals("0.5.14", apply.versionStaged().orElseThrow());

        Path stagedFile = updateDir.resolve("Croppy-0.5.13.jar");
        assertTrue(Files.exists(stagedFile));
        assertArrayEquals(jarBytes, assertDoesNotThrow(() -> Files.readAllBytes(stagedFile)));
    }

    @Test
    @DisplayName("Should abort and delete temp file if SHA-256 verification fails")
    void testApplyChecksumMismatch(@TempDir Path tempDir) {
        byte[] jarBytes = "corrupted-or-altered-jar-bytes".getBytes(StandardCharsets.UTF_8);
        String incorrectHash = "1111111111111111111111111111111111111111111111111111111111111111";

        String releaseJson = """
                {
                  "tag_name": "v0.5.14",
                  "html_url": "https://github.com/Cypherlink-Studios/Croppy/releases/tag/v0.5.14",
                  "published_at": "2026-11-08T12:00:00Z",
                  "assets": [
                    {
                      "name": "Croppy-0.5.14.jar",
                      "browser_download_url": "http://127.0.0.1:%d/download/Croppy-0.5.14.jar"
                    },
                    {
                      "name": "checksums-sha256.txt",
                      "browser_download_url": "http://127.0.0.1:%d/download/checksums-sha256.txt"
                    }
                  ]
                }
                """.formatted(port, port);

        String manifestContent = incorrectHash + "  Croppy-0.5.14.jar\n";

        server.createContext("/repos/Cypherlink-Studios/Croppy/releases/latest", exchange -> {
            byte[] b = releaseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        });

        server.createContext("/download/Croppy-0.5.14.jar", exchange -> {
            exchange.sendResponseHeaders(200, jarBytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(jarBytes); }
        });

        server.createContext("/download/checksums-sha256.txt", exchange -> {
            byte[] b = manifestContent.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        });

        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                "http://127.0.0.1:" + port,
                "Cypherlink-Studios/Croppy",
                SemanticVersion.of(0, 5, 13),
                Duration.ofMinutes(15)
        );

        Path updateDir = tempDir.resolve("update");
        PaperUpdateService service = new PaperUpdateService(
                client,
                updateDir,
                () -> "Croppy.jar",
                UpdateChannel.STABLE,
                true,
                Logger.getLogger("test")
        );

        UpdateApplyResult apply = service.applyUpdateAsync().join();
        assertFalse(apply.success());
        assertTrue(apply.errorMessage().orElseThrow().contains("verification failed"));

        // Staged file should NOT exist
        assertFalse(Files.exists(updateDir.resolve("Croppy.jar")));
        // Temp file should be cleaned up
        assertFalse(Files.exists(updateDir.resolve(".Croppy.jar.tmp")));
    }
}
