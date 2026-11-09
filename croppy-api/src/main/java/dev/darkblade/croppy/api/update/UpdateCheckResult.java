package dev.darkblade.croppy.api.update;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result record representing the outcome of an update check.
 */
public record UpdateCheckResult(
        boolean updateAvailable,
        SemanticVersion currentVersion,
        Optional<SemanticVersion> latestVersion,
        Optional<String> releaseTag,
        Optional<String> releaseNotesUrl,
        Optional<String> downloadUrl,
        Optional<String> checksumUrl,
        Optional<String> publishedAt,
        Optional<String> errorMessage
) {

    public UpdateCheckResult {
        Objects.requireNonNull(currentVersion, "currentVersion cannot be null");
        Objects.requireNonNull(latestVersion, "latestVersion cannot be null");
        Objects.requireNonNull(releaseTag, "releaseTag cannot be null");
        Objects.requireNonNull(releaseNotesUrl, "releaseNotesUrl cannot be null");
        Objects.requireNonNull(downloadUrl, "downloadUrl cannot be null");
        Objects.requireNonNull(checksumUrl, "checksumUrl cannot be null");
        Objects.requireNonNull(publishedAt, "publishedAt cannot be null");
        Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
    }

    public static UpdateCheckResult upToDate(SemanticVersion currentVersion) {
        return new UpdateCheckResult(
                false,
                currentVersion,
                Optional.of(currentVersion),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public static UpdateCheckResult available(
            SemanticVersion currentVersion,
            SemanticVersion latestVersion,
            String releaseTag,
            String releaseNotesUrl,
            String downloadUrl,
            String checksumUrl,
            String publishedAt
    ) {
        return new UpdateCheckResult(
                true,
                currentVersion,
                Optional.of(latestVersion),
                Optional.ofNullable(releaseTag),
                Optional.ofNullable(releaseNotesUrl),
                Optional.ofNullable(downloadUrl),
                Optional.ofNullable(checksumUrl),
                Optional.ofNullable(publishedAt),
                Optional.empty()
        );
    }

    public static UpdateCheckResult failed(SemanticVersion currentVersion, String errorMessage) {
        return new UpdateCheckResult(
                false,
                currentVersion,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(errorMessage)
        );
    }

    public boolean isFailed() {
        return errorMessage.isPresent();
    }
}
