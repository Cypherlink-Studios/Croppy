package dev.darkblade.croppy.platform.paper.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.core.i18n.LocaleValidationReport;
import dev.darkblade.croppy.core.i18n.LocaleValidator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class LocaleResourcesIntegrityTest {

    @Test
    void testBundledLocalesIntegrity() {
        Path localesDir = Paths.get("src", "main", "resources", "locales");
        LocaleValidator validator = new LocaleValidator();

        LocaleValidationReport report = validator.validateDirectory(localesDir, LocaleRef.EN_US);

        System.out.println(report.formatSummary());

        assertFalse(report.hasErrors(), "Bundled locales have errors: " + report.syntaxErrors());
        assertTrue(report.sourceKeyCount() > 0, "Source locale must have keys");
        assertTrue(report.missingKeysPerLocale().isEmpty(), "Bundled secondary locales have missing keys: " + report.missingKeysPerLocale());
        assertTrue(report.emptyValuesPerLocale().isEmpty(), "Bundled locales have empty values: " + report.emptyValuesPerLocale());
        assertTrue(report.isClean(), "Bundled locales must be 100% clean and synchronized");
    }
}
