package dev.darkblade.croppy.core.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Report containing validation findings across locale translation catalogs.
 */
public record LocaleValidationReport(
        LocaleRef sourceLocale,
        int sourceKeyCount,
        Map<LocaleRef, Set<String>> missingKeysPerLocale,
        Map<LocaleRef, Set<String>> extraKeysPerLocale,
        Map<LocaleRef, Set<String>> emptyValuesPerLocale,
        List<String> syntaxErrors
) {

    public static LocaleValidationReport empty(LocaleRef sourceLocale) {
        return new LocaleValidationReport(
                sourceLocale,
                0,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyList()
        );
    }

    public boolean hasErrors() {
        return !syntaxErrors.isEmpty() || sourceKeyCount == 0;
    }

    public boolean hasWarnings() {
        return !missingKeysPerLocale.isEmpty() || !extraKeysPerLocale.isEmpty() || !emptyValuesPerLocale.isEmpty();
    }

    public boolean isClean() {
        return !hasErrors() && !hasWarnings();
    }

    public String formatSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Localization Validation Report ===\n");
        sb.append("Source Locale: ").append(sourceLocale).append(" (").append(sourceKeyCount).append(" keys)\n");

        if (!syntaxErrors.isEmpty()) {
            sb.append("\n[ERRORS - Syntax / Load Failures]:\n");
            for (String err : syntaxErrors) {
                sb.append("  ✖ ").append(err).append("\n");
            }
        }

        if (!missingKeysPerLocale.isEmpty()) {
            sb.append("\n[WARNINGS - Missing Keys in Target Locales]:\n");
            for (Map.Entry<LocaleRef, Set<String>> entry : missingKeysPerLocale.entrySet()) {
                sb.append("  Locale [").append(entry.getKey()).append("] missing ").append(entry.getValue().size()).append(" keys:\n");
                for (String k : entry.getValue().stream().limit(10).toList()) {
                    sb.append("    - ").append(k).append("\n");
                }
                if (entry.getValue().size() > 10) {
                    sb.append("    ... and ").append(entry.getValue().size() - 10).append(" more\n");
                }
            }
        }

        if (!emptyValuesPerLocale.isEmpty()) {
            sb.append("\n[WARNINGS - Empty Values in Locales]:\n");
            for (Map.Entry<LocaleRef, Set<String>> entry : emptyValuesPerLocale.entrySet()) {
                sb.append("  Locale [").append(entry.getKey()).append("] has ").append(entry.getValue().size()).append(" empty keys\n");
            }
        }

        if (!extraKeysPerLocale.isEmpty()) {
            sb.append("\n[INFO - Extra Keys not in Source]:\n");
            for (Map.Entry<LocaleRef, Set<String>> entry : extraKeysPerLocale.entrySet()) {
                sb.append("  Locale [").append(entry.getKey()).append("] has ").append(entry.getValue().size()).append(" unreferenced keys\n");
            }
        }

        if (isClean()) {
            sb.append("✔ All locales are 100% synchronized and valid.\n");
        }
        return sb.toString();
    }
}
