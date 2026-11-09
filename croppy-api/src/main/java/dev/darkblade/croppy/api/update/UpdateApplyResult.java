package dev.darkblade.croppy.api.update;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result record representing the outcome of applying/staging an update.
 */
public record UpdateApplyResult(
        boolean success,
        boolean alreadyUpToDate,
        Optional<String> stagedFileName,
        Optional<String> versionStaged,
        Optional<String> message,
        Optional<String> errorMessage
) {

    public UpdateApplyResult {
        Objects.requireNonNull(stagedFileName, "stagedFileName cannot be null");
        Objects.requireNonNull(versionStaged, "versionStaged cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
    }

    public static UpdateApplyResult success(String stagedFileName, String versionStaged, String message) {
        return new UpdateApplyResult(
                true,
                false,
                Optional.ofNullable(stagedFileName),
                Optional.ofNullable(versionStaged),
                Optional.ofNullable(message),
                Optional.empty()
        );
    }

    public static UpdateApplyResult alreadyUpToDate(String message) {
        return new UpdateApplyResult(
                true,
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(message),
                Optional.empty()
        );
    }

    public static UpdateApplyResult failed(String errorMessage) {
        return new UpdateApplyResult(
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(errorMessage)
        );
    }
}
