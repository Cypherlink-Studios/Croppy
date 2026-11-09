package dev.darkblade.croppy.platform.paper.update;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumValidatorTest {

    private static final String MANIFEST_SAMPLE = """
            # Cryptographic checksums for release v0.5.14
            e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855  Croppy-0.5.14.jar
            b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9 *Croppy-ResourcePack-0.5.14.zip
            d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2   ./croppy-api-0.5.14.jar
            """;

    @Test
    @DisplayName("Should extract expected hash from various sha256 manifest line formats")
    void testExtractExpectedHash() {
        Optional<String> jarHash = ChecksumValidator.extractExpectedHash(MANIFEST_SAMPLE, "Croppy-0.5.14.jar");
        assertTrue(jarHash.isPresent());
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", jarHash.get());

        // Asterisk binary prefix
        Optional<String> packHash = ChecksumValidator.extractExpectedHash(MANIFEST_SAMPLE, "Croppy-ResourcePack-0.5.14.zip");
        assertTrue(packHash.isPresent());
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", packHash.get());

        // Relative path prefix
        Optional<String> apiHash = ChecksumValidator.extractExpectedHash(MANIFEST_SAMPLE, "croppy-api-0.5.14.jar");
        assertTrue(apiHash.isPresent());
        assertEquals("d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2", apiHash.get());
    }

    @Test
    @DisplayName("Should return empty when target file is not in manifest")
    void testExtractMissingFile() {
        Optional<String> missing = ChecksumValidator.extractExpectedHash(MANIFEST_SAMPLE, "non-existent.jar");
        assertTrue(missing.isEmpty());

        assertTrue(ChecksumValidator.extractExpectedHash("", "Croppy.jar").isEmpty());
        assertTrue(ChecksumValidator.extractExpectedHash(null, "Croppy.jar").isEmpty());
    }

    @Test
    @DisplayName("Should compute standard SHA-256 digests accurately")
    void testComputeSha256() {
        // SHA-256 of empty byte array
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                ChecksumValidator.computeSha256(new byte[0]));

        // SHA-256 of "hello world"
        byte[] helloWorld = "hello world".getBytes(StandardCharsets.UTF_8);
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
                ChecksumValidator.computeSha256(helloWorld));
    }

    @Test
    @DisplayName("Should verify byte data and files on disk against expected hash")
    void testVerifyFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test-binary.jar");
        byte[] content = "valid-jar-binary-stream".getBytes(StandardCharsets.UTF_8);
        Files.write(file, content);

        String correctHash = ChecksumValidator.computeSha256(content);
        assertTrue(ChecksumValidator.verify(content, correctHash));
        assertTrue(ChecksumValidator.verify(file, correctHash));

        // Corrupted check
        String corruptedHash = "0000000000000000000000000000000000000000000000000000000000000000";
        assertFalse(ChecksumValidator.verify(content, corruptedHash));
        assertFalse(ChecksumValidator.verify(file, corruptedHash));
    }
}
