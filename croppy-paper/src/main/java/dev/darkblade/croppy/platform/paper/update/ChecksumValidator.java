package dev.darkblade.croppy.platform.paper.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility for parsing sha256 checksum manifests and verifying file digests.
 */
public final class ChecksumValidator {

    private ChecksumValidator() {}

    /**
     * Extracts the expected SHA-256 hash for a specific file name from a sha256sum manifest.
     *
     * @param manifestContent the raw text content of checksums-sha256.txt
     * @param targetFileName the name of the file to resolve (e.g. "Croppy-0.5.14.jar")
     * @return an Optional containing the expected 64-character lowercase hexadecimal hash, or empty
     */
    public static Optional<String> extractExpectedHash(String manifestContent, String targetFileName) {
        if (manifestContent == null || manifestContent.isBlank() || targetFileName == null || targetFileName.isBlank()) {
            return Optional.empty();
        }

        String targetNameOnly = sanitizeFileName(targetFileName);

        String[] lines = manifestContent.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // Standard sha256sum format: "<hash>  <filename>" or "<hash> *<filename>" (binary mode)
            String[] tokens = trimmed.split("\\s+", 2);
            if (tokens.length == 2) {
                String hash = tokens[0].trim().toLowerCase(Locale.ROOT);
                String fileName = tokens[1].trim();
                if (fileName.startsWith("*")) {
                    fileName = fileName.substring(1).trim();
                }
                String baseFileName = sanitizeFileName(fileName);

                if (baseFileName.equalsIgnoreCase(targetNameOnly) && hash.length() == 64) {
                    return Optional.of(hash);
                }
            }
        }

        return Optional.empty();
    }

    private static String sanitizeFileName(String input) {
        if (input == null) return "";
        String s = input.trim();
        if (s.contains("/")) {
            s = s.substring(s.lastIndexOf('/') + 1);
        }
        if (s.contains("\\")) {
            s = s.substring(s.lastIndexOf('\\') + 1);
        }
        return s;
    }

    /**
     * Computes the SHA-256 digest of a byte array and formats it as a 64-character lowercase hex string.
     *
     * @param data the byte array to hash
     * @return the SHA-256 hex string
     */
    public static String computeSha256(byte[] data) {
        Objects.requireNonNull(data, "data cannot be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    /**
     * Computes the SHA-256 digest of a file on disk.
     *
     * @param filePath the path of the file to hash
     * @return the SHA-256 hex string
     * @throws IOException if reading the file fails
     */
    public static String computeSha256(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(filePath);
                 DigestInputStream dis = new DigestInputStream(is, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // stream through to compute digest
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    /**
     * Verifies that the computed SHA-256 hash of a file matches the expected hash.
     *
     * @param filePath the path of the file to verify
     * @param expectedHash the expected 64-character hex hash
     * @return true if hashes match
     * @throws IOException if reading the file fails
     */
    public static boolean verify(Path filePath, String expectedHash) throws IOException {
        if (expectedHash == null || expectedHash.isBlank()) {
            return false;
        }
        String computed = computeSha256(filePath);
        return computed.equalsIgnoreCase(expectedHash.trim());
    }

    /**
     * Verifies that the computed SHA-256 hash of byte data matches the expected hash.
     *
     * @param data the byte array to verify
     * @param expectedHash the expected 64-character hex hash
     * @return true if hashes match
     */
    public static boolean verify(byte[] data, String expectedHash) {
        if (expectedHash == null || expectedHash.isBlank()) {
            return false;
        }
        String computed = computeSha256(data);
        return computed.equalsIgnoreCase(expectedHash.trim());
    }
}
