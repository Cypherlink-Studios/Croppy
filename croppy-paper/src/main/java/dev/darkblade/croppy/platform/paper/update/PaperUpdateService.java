package dev.darkblade.croppy.platform.paper.update;

import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.update.SemanticVersion;
import dev.darkblade.croppy.api.update.UpdateApplyResult;
import dev.darkblade.croppy.api.update.UpdateChannel;
import dev.darkblade.croppy.api.update.UpdateCheckResult;
import dev.darkblade.croppy.api.update.UpdateService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Paper platform implementation of the UpdateService port.
 * Coordinates asynchronous GitHub release queries, SHA-256 integrity verification,
 * and safe staging to Paper's plugins/update/ directory.
 */
public class PaperUpdateService implements UpdateService {

    private final GitHubReleaseClient releaseClient;
    private final Path updateDirectory;
    private final Supplier<String> targetJarNameSupplier;
    private final UpdateChannel channel;
    private final boolean enabled;
    private final CroppyLogger logger;

    public PaperUpdateService(
            GitHubReleaseClient releaseClient,
            Path updateDirectory,
            Supplier<String> targetJarNameSupplier,
            UpdateChannel channel,
            boolean enabled,
            Logger logger
    ) {
        this(
                releaseClient,
                updateDirectory,
                targetJarNameSupplier,
                channel,
                enabled,
                logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE
        );
    }

    public PaperUpdateService(
            GitHubReleaseClient releaseClient,
            Path updateDirectory,
            Supplier<String> targetJarNameSupplier,
            UpdateChannel channel,
            boolean enabled,
            CroppyLogger logger
    ) {
        this.releaseClient = Objects.requireNonNull(releaseClient, "releaseClient cannot be null");
        this.updateDirectory = Objects.requireNonNull(updateDirectory, "updateDirectory cannot be null");
        this.targetJarNameSupplier = Objects.requireNonNull(targetJarNameSupplier, "targetJarNameSupplier cannot be null");
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
        this.enabled = enabled;
        this.logger = (logger != null) ? logger : NoOpCroppyLogger.INSTANCE;
    }

    @Override
    public CompletableFuture<UpdateCheckResult> checkForUpdatesAsync(boolean forceCheck) {
        if (!enabled) {
            return CompletableFuture.completedFuture(
                    UpdateCheckResult.failed(SemanticVersion.of(0, 0, 0), "Update checking is disabled in configuration")
            );
        }
        return releaseClient.checkLatestRelease(channel, forceCheck);
    }

    @Override
    public CompletableFuture<UpdateApplyResult> applyUpdateAsync() {
        if (!enabled) {
            return CompletableFuture.completedFuture(
                    UpdateApplyResult.failed("Update subsystem is disabled in configuration")
            );
        }

        return checkForUpdatesAsync(false).thenCompose(checkResult -> {
            if (checkResult.isFailed()) {
                return CompletableFuture.completedFuture(
                        UpdateApplyResult.failed(checkResult.errorMessage().orElse("Unknown error during update check"))
                );
            }

            if (!checkResult.updateAvailable()) {
                return CompletableFuture.completedFuture(
                        UpdateApplyResult.alreadyUpToDate("Server is already running the latest version (" + checkResult.currentVersion().asString() + ")")
                );
            }

            String jarDownloadUrl = checkResult.downloadUrl().orElse("");
            if (jarDownloadUrl.isBlank()) {
                return CompletableFuture.completedFuture(
                        UpdateApplyResult.failed("Release " + checkResult.releaseTag().orElse("?") + " does not contain a compatible Croppy JAR asset")
                );
            }

            String targetJarName = targetJarNameSupplier.get();
            if (targetJarName == null || targetJarName.isBlank()) {
                targetJarName = "Croppy.jar";
            }

            final String finalTargetJarName = targetJarName;
            final SemanticVersion latestVersion = checkResult.latestVersion().orElse(checkResult.currentVersion());

            // 1. Download manifest & JAR binary concurrently
            CompletableFuture<String> manifestFuture = resolveChecksumManifest(checkResult);
            CompletableFuture<byte[]> jarFuture = releaseClient.downloadAsset(jarDownloadUrl);

            return manifestFuture.thenCombine(jarFuture, (manifestContent, jarBytes) -> {
                try {
                    Files.createDirectories(updateDirectory);

                    Path tempFile = updateDirectory.resolve("." + finalTargetJarName + ".tmp");
                    Path targetFile = updateDirectory.resolve(finalTargetJarName);

                    // Write temporary download file
                    Files.write(tempFile, jarBytes);

                    // Resolve remote asset name (e.g. Croppy-0.5.14.jar) from URL safely
                    String uriPath = java.net.URI.create(jarDownloadUrl).getPath();
                    String remoteAssetName = (uriPath != null && uriPath.contains("/"))
                            ? uriPath.substring(uriPath.lastIndexOf('/') + 1)
                            : (uriPath != null ? uriPath : "Croppy.jar");
                    Optional<String> expectedHash = ChecksumValidator.extractExpectedHash(manifestContent, remoteAssetName);

                    if (expectedHash.isPresent()) {
                        String computedHash = ChecksumValidator.computeSha256(tempFile);
                        if (!computedHash.equalsIgnoreCase(expectedHash.get())) {
                            Files.deleteIfExists(tempFile);
                            logger.error(LogChannel.UPDATE, "Checksum mismatch for downloaded update! Expected: " + expectedHash.get() + ", got: " + computedHash);
                            return UpdateApplyResult.failed("Cryptographic verification failed: SHA-256 digest does not match release manifest");
                        }
                    } else {
                        logger.warn(LogChannel.UPDATE, "Manifest did not contain checksum for asset " + remoteAssetName + ". Proceeding with caution.");
                    }

                    // Move to plugins/update/<currentJarName>
                    try {
                        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException atomicMoveUnsupported) {
                        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    logger.info(LogChannel.UPDATE, "Successfully staged Croppy v" + latestVersion.asString() + " to " + targetFile);
                    return UpdateApplyResult.success(
                            finalTargetJarName,
                            latestVersion.asString(),
                            "Update successfully staged to plugins/update/" + finalTargetJarName + ". Restart server to apply."
                    );
                } catch (IOException e) {
                    logger.error(LogChannel.UPDATE, "Failed to stage update file on disk", e);
                    return UpdateApplyResult.failed("Failed to stage update file on disk: " + e.getMessage());
                }
            }).exceptionally(throwable -> {
                String msg = (throwable.getCause() != null) ? throwable.getCause().getMessage() : throwable.getMessage();
                logger.error(LogChannel.UPDATE, "Exception occurred during update application", throwable);
                return UpdateApplyResult.failed("Update download failed: " + msg);
            });
        });
    }

    private CompletableFuture<String> resolveChecksumManifest(UpdateCheckResult checkResult) {
        if (checkResult.checksumUrl().isPresent() && !checkResult.checksumUrl().get().isBlank()) {
            return releaseClient.fetchText(checkResult.checksumUrl().get());
        }

        String tag = checkResult.releaseTag().orElse("");
        if (!tag.isBlank()) {
            String fallbackUrl = "https://github.com/" + releaseClient.getRepository() + "/releases/download/" + tag + "/checksums-sha256.txt";
            return releaseClient.fetchText(fallbackUrl).exceptionally(err -> "");
        }

        return CompletableFuture.completedFuture("");
    }

    @Override
    public Optional<UpdateCheckResult> getCachedResult() {
        return releaseClient.getCachedResult();
    }

    @Override
    public UpdateChannel getChannel() {
        return channel;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
