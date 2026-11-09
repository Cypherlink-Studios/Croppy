package dev.darkblade.croppy.platform.paper.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkblade.croppy.api.update.SemanticVersion;
import dev.darkblade.croppy.api.update.UpdateChannel;
import dev.darkblade.croppy.api.update.UpdateCheckResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous HTTP client for querying and downloading releases from GitHub Releases API.
 */
public class GitHubReleaseClient {

    public static final String DEFAULT_BASE_URL = "https://api.github.com";
    public static final String DEFAULT_REPOSITORY = "Cypherlink-Studios/Croppy";
    public static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(15);
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String repository;
    private final SemanticVersion currentVersion;
    private final Duration cacheTtl;

    private volatile CachedCheck cachedCheck;

    private record CachedCheck(Instant timestamp, UpdateChannel channel, UpdateCheckResult result) {
        boolean isValid(Duration ttl, UpdateChannel requestedChannel) {
            return this.channel == requestedChannel && Duration.between(timestamp, Instant.now()).compareTo(ttl) < 0;
        }
    }

    public GitHubReleaseClient(
            HttpClient httpClient,
            String baseUrl,
            String repository,
            SemanticVersion currentVersion,
            Duration cacheTtl
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion cannot be null");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl cannot be null");
    }

    public GitHubReleaseClient(HttpClient httpClient, SemanticVersion currentVersion) {
        this(httpClient, DEFAULT_BASE_URL, DEFAULT_REPOSITORY, currentVersion, DEFAULT_CACHE_TTL);
    }

    public String getRepository() {
        return repository;
    }

    public Optional<UpdateCheckResult> getCachedResult() {
        CachedCheck current = this.cachedCheck;
        if (current != null && current.isValid(cacheTtl, current.channel())) {
            return Optional.of(current.result());
        }
        return Optional.empty();
    }

    public CompletableFuture<UpdateCheckResult> checkLatestRelease(UpdateChannel channel, boolean force) {
        if (!force) {
            CachedCheck current = this.cachedCheck;
            if (current != null && current.isValid(cacheTtl, channel)) {
                return CompletableFuture.completedFuture(current.result());
            }
        }

        String endpoint = (channel == UpdateChannel.STABLE)
                ? baseUrl + "/repos/" + repository + "/releases/latest"
                : baseUrl + "/repos/" + repository + "/releases";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("User-Agent", "Croppy-Updater/" + currentVersion.asString())
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleReleaseResponse(response, channel))
                .exceptionally(throwable -> {
                    String error = (throwable.getCause() != null) ? throwable.getCause().getMessage() : throwable.getMessage();
                    return UpdateCheckResult.failed(currentVersion, "Network error contacting update server: " + error);
                });
    }

    public CompletableFuture<byte[]> downloadAsset(String assetUrl) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(assetUrl))
                .header("User-Agent", "Croppy-Updater/" + currentVersion.asString())
                .header("Accept", "application/octet-stream")
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(response.body());
                    }
                    return CompletableFuture.failedFuture(
                            new IllegalStateException("Failed to download asset, HTTP status: " + response.statusCode())
                    );
                });
    }

    public CompletableFuture<String> fetchText(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Croppy-Updater/" + currentVersion.asString())
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(response.body());
                    }
                    return CompletableFuture.failedFuture(
                            new IllegalStateException("Failed to fetch text from " + url + ", HTTP status: " + response.statusCode())
                    );
                });
    }

    private UpdateCheckResult handleReleaseResponse(HttpResponse<String> response, UpdateChannel channel) {
        int statusCode = response.statusCode();

        if (statusCode == 403) {
            Optional<String> resetHeader = response.headers().firstValue("x-ratelimit-reset");
            String waitInfo = resetHeader.map(r -> " (reset timestamp: " + r + ")").orElse("");
            return UpdateCheckResult.failed(currentVersion, "GitHub API rate limit exceeded" + waitInfo);
        }

        if (statusCode == 404) {
            return UpdateCheckResult.failed(currentVersion, "No published releases found for " + repository);
        }

        if (statusCode < 200 || statusCode >= 300) {
            return UpdateCheckResult.failed(currentVersion, "GitHub API returned unexpected HTTP status: " + statusCode);
        }

        try {
            JsonObject targetRelease;
            if (channel == UpdateChannel.STABLE) {
                targetRelease = JsonParser.parseString(response.body()).getAsJsonObject();
            } else {
                JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();
                if (releases.isEmpty()) {
                    return UpdateCheckResult.failed(currentVersion, "No releases found on channel " + channel);
                }
                // Pick the first non-draft release
                JsonObject found = null;
                for (JsonElement el : releases) {
                    JsonObject rel = el.getAsJsonObject();
                    if (!rel.has("draft") || !rel.get("draft").getAsBoolean()) {
                        found = rel;
                        break;
                    }
                }
                if (found == null) {
                    return UpdateCheckResult.failed(currentVersion, "No non-draft releases found on channel " + channel);
                }
                targetRelease = found;
            }

            String tagName = targetRelease.get("tag_name").getAsString();
            String htmlUrl = targetRelease.has("html_url") ? targetRelease.get("html_url").getAsString() : "";
            String publishedAt = targetRelease.has("published_at") ? targetRelease.get("published_at").getAsString() : "";

            SemanticVersion remoteVersion = SemanticVersion.parse(tagName);

            if (remoteVersion.isNewerThan(currentVersion)) {
                String downloadUrl = "";
                String checksumUrl = "";
                if (targetRelease.has("assets") && targetRelease.get("assets").isJsonArray()) {
                    JsonArray assets = targetRelease.getAsJsonArray("assets");
                    for (JsonElement assetElem : assets) {
                        JsonObject asset = assetElem.getAsJsonObject();
                        String name = asset.get("name").getAsString();
                        if (isTargetPluginJar(name)) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                        } else if ("checksums-sha256.txt".equalsIgnoreCase(name)) {
                            checksumUrl = asset.get("browser_download_url").getAsString();
                        }
                    }
                }

                UpdateCheckResult result = UpdateCheckResult.available(
                        currentVersion,
                        remoteVersion,
                        tagName,
                        htmlUrl,
                        downloadUrl,
                        checksumUrl,
                        publishedAt
                );
                this.cachedCheck = new CachedCheck(Instant.now(), channel, result);
                return result;
            } else {
                UpdateCheckResult result = UpdateCheckResult.upToDate(currentVersion);
                this.cachedCheck = new CachedCheck(Instant.now(), channel, result);
                return result;
            }
        } catch (Exception e) {
            return UpdateCheckResult.failed(currentVersion, "Failed to parse release metadata: " + e.getMessage());
        }
    }

    private static boolean isTargetPluginJar(String assetName) {
        if (assetName == null) return false;
        if (assetName.contains("ResourcePack") || assetName.contains("croppy-api")) {
            return false;
        }
        return assetName.startsWith("Croppy-") && assetName.endsWith(".jar");
    }
}
