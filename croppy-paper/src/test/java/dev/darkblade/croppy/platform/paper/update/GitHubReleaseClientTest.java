package dev.darkblade.croppy.platform.paper.update;

import com.sun.net.httpserver.HttpServer;
import dev.darkblade.croppy.api.update.SemanticVersion;
import dev.darkblade.croppy.api.update.UpdateChannel;
import dev.darkblade.croppy.api.update.UpdateCheckResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GitHubReleaseClientTest {

    private HttpServer server;
    private int port;
    private AtomicInteger requestCounter;

    @BeforeEach
    void setUp() throws IOException {
        requestCounter = new AtomicInteger(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Should detect newer stable release and parse download url")
    void testDetectNewerStableRelease() {
        String json = """
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
                      "name": "Croppy-ResourcePack-0.5.14.zip",
                      "browser_download_url": "http://127.0.0.1:%d/download/pack.zip"
                    }
                  ]
                }
                """.formatted(port, port);

        server.createContext("/repos/Cypherlink-Studios/Croppy/releases/latest", exchange -> {
            requestCounter.incrementAndGet();
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        HttpClient httpClient = HttpClient.newHttpClient();
        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                "http://127.0.0.1:" + port,
                "Cypherlink-Studios/Croppy",
                SemanticVersion.parse("0.5.13"),
                Duration.ofMinutes(15)
        );

        UpdateCheckResult result = client.checkLatestRelease(UpdateChannel.STABLE, false).join();

        assertTrue(result.updateAvailable());
        assertEquals("0.5.14", result.latestVersion().orElseThrow().asString());
        assertEquals("v0.5.14", result.releaseTag().orElseThrow());
        assertTrue(result.downloadUrl().orElseThrow().endsWith("Croppy-0.5.14.jar"));
        assertEquals(1, requestCounter.get());

        // Cache test: second request with force=false shouldn't hit HTTP server
        UpdateCheckResult cached = client.checkLatestRelease(UpdateChannel.STABLE, false).join();
        assertEquals(1, requestCounter.get());
        assertTrue(cached.updateAvailable());
    }

    @Test
    @DisplayName("Should report up to date when current version matches remote")
    void testUpToDate() {
        String json = """
                {
                  "tag_name": "v0.5.13",
                  "html_url": "https://github.com/Cypherlink-Studios/Croppy/releases/tag/v0.5.13",
                  "published_at": "2026-11-08T12:00:00Z",
                  "assets": []
                }
                """;

        server.createContext("/repos/Cypherlink-Studios/Croppy/releases/latest", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        HttpClient httpClient = HttpClient.newHttpClient();
        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                "http://127.0.0.1:" + port,
                "Cypherlink-Studios/Croppy",
                SemanticVersion.parse("0.5.13"),
                Duration.ofMinutes(15)
        );

        UpdateCheckResult result = client.checkLatestRelease(UpdateChannel.STABLE, false).join();
        assertFalse(result.updateAvailable());
        assertFalse(result.isFailed());
    }

    @Test
    @DisplayName("Should detect beta pre-releases on BETA channel")
    void testBetaChannelReleases() {
        String json = """
                [
                  {
                    "tag_name": "v0.5.14-beta.1",
                    "draft": false,
                    "prerelease": true,
                    "assets": [
                      {
                        "name": "Croppy-0.5.14-beta.1.jar",
                        "browser_download_url": "http://127.0.0.1:%d/download/beta.jar"
                      }
                    ]
                  }
                ]
                """.formatted(port);

        server.createContext("/repos/Cypherlink-Studios/Croppy/releases", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        HttpClient httpClient = HttpClient.newHttpClient();
        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                "http://127.0.0.1:" + port,
                "Cypherlink-Studios/Croppy",
                SemanticVersion.parse("0.5.13"),
                Duration.ofMinutes(15)
        );

        UpdateCheckResult result = client.checkLatestRelease(UpdateChannel.BETA, false).join();
        assertTrue(result.updateAvailable());
        assertEquals("0.5.14-beta.1", result.latestVersion().orElseThrow().asString());
    }

    @Test
    @DisplayName("Should handle GitHub rate limit response gracefully")
    void testRateLimitHandling() {
        server.createContext("/repos/Cypherlink-Studios/Croppy/releases/latest", exchange -> {
            exchange.getResponseHeaders().set("x-ratelimit-reset", "1762600000");
            exchange.sendResponseHeaders(403, 0);
            exchange.close();
        });

        HttpClient httpClient = HttpClient.newHttpClient();
        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                "http://127.0.0.1:" + port,
                "Cypherlink-Studios/Croppy",
                SemanticVersion.parse("0.5.13"),
                Duration.ofMinutes(15)
        );

        UpdateCheckResult result = client.checkLatestRelease(UpdateChannel.STABLE, false).join();
        assertTrue(result.isFailed());
        assertTrue(result.errorMessage().orElseThrow().contains("rate limit"));
    }

    @Test
    @DisplayName("Should download raw asset bytes")
    void testDownloadAsset() {
        byte[] expected = "fake-jar-binary-stream".getBytes(StandardCharsets.UTF_8);
        server.createContext("/download/test.jar", exchange -> {
            exchange.sendResponseHeaders(200, expected.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(expected);
            }
        });

        HttpClient httpClient = HttpClient.newHttpClient();
        GitHubReleaseClient client = new GitHubReleaseClient(
                httpClient,
                SemanticVersion.parse("0.5.13")
        );

        byte[] downloaded = client.downloadAsset("http://127.0.0.1:" + port + "/download/test.jar").join();
        assertArrayEquals(expected, downloaded);
    }
}
