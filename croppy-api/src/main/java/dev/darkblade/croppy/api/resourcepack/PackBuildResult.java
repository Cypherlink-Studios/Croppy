package dev.darkblade.croppy.api.resourcepack;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable summary result of a compiled companion resource pack.
 *
 * @param zipPath destination path of the generated ZIP file
 * @param sha1Hex 40-character hexadecimal representation of the SHA-1 checksum
 * @param sha1Digest raw 20-byte SHA-1 digest array
 * @param sizeBytes size of the generated ZIP file in bytes
 * @param assetCount total number of individual asset files merged into the pack
 * @param durationMs compilation elapsed time in milliseconds
 */
public record PackBuildResult(
        Path zipPath,
        String sha1Hex,
        byte[] sha1Digest,
        long sizeBytes,
        int assetCount,
        long durationMs
) {
    public PackBuildResult {
        Objects.requireNonNull(zipPath, "zipPath cannot be null");
        Objects.requireNonNull(sha1Hex, "sha1Hex cannot be null");
        Objects.requireNonNull(sha1Digest, "sha1Digest cannot be null");
        sha1Digest = Arrays.copyOf(sha1Digest, sha1Digest.length);
    }

    @Override
    public byte[] sha1Digest() {
        return Arrays.copyOf(sha1Digest, sha1Digest.length);
    }
}
