package dev.darkblade.croppy.core.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocaleValidatorTest {

    private final LocaleValidator validator = new LocaleValidator();

    @Test
    void testCleanLocales() {
        Map<String, String> enUS = Map.of(
                "key1", "Val 1",
                "key2", "Val 2"
        );
        Map<String, String> esES = Map.of(
                "key1", "Val 1 ES",
                "key2", "Val 2 ES"
        );

        LocaleValidationReport report = validator.validate(LocaleRef.EN_US, Map.of(
                LocaleRef.EN_US, enUS,
                LocaleRef.ES_ES, esES
        ));

        assertTrue(report.isClean());
        assertFalse(report.hasErrors());
        assertFalse(report.hasWarnings());
        assertEquals(2, report.sourceKeyCount());
    }

    @Test
    void testMissingKeysInSecondary() {
        Map<String, String> enUS = Map.of(
                "key1", "Val 1",
                "key2", "Val 2"
        );
        Map<String, String> esES = Map.of(
                "key1", "Val 1 ES"
        );

        LocaleValidationReport report = validator.validate(LocaleRef.EN_US, Map.of(
                LocaleRef.EN_US, enUS,
                LocaleRef.ES_ES, esES
        ));

        assertFalse(report.hasErrors());
        assertTrue(report.hasWarnings());
        assertTrue(report.missingKeysPerLocale().containsKey(LocaleRef.ES_ES));
        assertTrue(report.missingKeysPerLocale().get(LocaleRef.ES_ES).contains("key2"));
    }

    @Test
    void testMissingSourceLocaleIsError() {
        Map<String, String> esES = Map.of("key1", "Val 1");

        LocaleValidationReport report = validator.validate(LocaleRef.EN_US, Map.of(
                LocaleRef.ES_ES, esES
        ));

        assertTrue(report.hasErrors());
        assertEquals(0, report.sourceKeyCount());
    }

    @Test
    void testDirectoryValidation(@TempDir Path tempDir) throws Exception {
        Path enFile = tempDir.resolve("en-US.yml");
        Path esFile = tempDir.resolve("es-ES.yml");

        Files.writeString(enFile, "app:\n  title: \"Croppy\"\n  desc: \"Farming\"\n");
        Files.writeString(esFile, "app:\n  title: \"Croppy\"\n"); // missing app.desc

        LocaleValidationReport report = validator.validateDirectory(tempDir, LocaleRef.EN_US);

        assertFalse(report.hasErrors());
        assertTrue(report.hasWarnings());
        assertEquals(2, report.sourceKeyCount());
        assertTrue(report.missingKeysPerLocale().get(LocaleRef.ES_ES).contains("app.desc"));
    }
}
