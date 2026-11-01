package dev.darkblade.croppy.core.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validates localization resource bundles, comparing secondary locales against the source locale.
 */
public class LocaleValidator {

    private final YamlLocaleLoader loader;

    public LocaleValidator(YamlLocaleLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader cannot be null");
    }

    public LocaleValidator() {
        this(new YamlLocaleLoader());
    }

    /**
     * Validates in-memory message mappings against the source locale.
     *
     * @param sourceLocale reference source locale (e.g. en-US)
     * @param locales      map of all loaded locales
     * @return validation report
     */
    public LocaleValidationReport validate(LocaleRef sourceLocale, Map<LocaleRef, Map<String, String>> locales) {
        Objects.requireNonNull(sourceLocale, "sourceLocale cannot be null");
        Objects.requireNonNull(locales, "locales cannot be null");

        Map<String, String> sourceMessages = locales.get(sourceLocale);
        if (sourceMessages == null || sourceMessages.isEmpty()) {
            return new LocaleValidationReport(
                    sourceLocale,
                    0,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    List.of("Source locale [" + sourceLocale + "] is missing or empty")
            );
        }

        Set<String> sourceKeys = sourceMessages.keySet();
        Map<LocaleRef, Set<String>> missingKeys = new HashMap<>();
        Map<LocaleRef, Set<String>> extraKeys = new HashMap<>();
        Map<LocaleRef, Set<String>> emptyValues = new HashMap<>();

        for (Map.Entry<LocaleRef, Map<String, String>> entry : locales.entrySet()) {
            LocaleRef targetLocale = entry.getKey();
            if (targetLocale.equals(sourceLocale)) {
                // Check if source locale has any empty values
                Set<String> emptyInSource = findEmptyValues(entry.getValue());
                if (!emptyInSource.isEmpty()) {
                    emptyValues.put(sourceLocale, emptyInSource);
                }
                continue;
            }

            Map<String, String> targetMessages = entry.getValue();

            // Find missing keys (in source but not in target)
            Set<String> missing = new TreeSet<>(sourceKeys);
            missing.removeAll(targetMessages.keySet());
            if (!missing.isEmpty()) {
                missingKeys.put(targetLocale, missing);
            }

            // Find extra keys (in target but not in source)
            Set<String> extra = new TreeSet<>(targetMessages.keySet());
            extra.removeAll(sourceKeys);
            if (!extra.isEmpty()) {
                extraKeys.put(targetLocale, extra);
            }

            // Find empty values
            Set<String> empty = findEmptyValues(targetMessages);
            if (!empty.isEmpty()) {
                emptyValues.put(targetLocale, empty);
            }
        }

        return new LocaleValidationReport(
                sourceLocale,
                sourceKeys.size(),
                Collections.unmodifiableMap(missingKeys),
                Collections.unmodifiableMap(extraKeys),
                Collections.unmodifiableMap(emptyValues),
                Collections.emptyList()
        );
    }

    /**
     * Inspects a directory on disk, parses all YAML files, and validates them.
     *
     * @param directory    directory containing locale YAML files
     * @param sourceLocale reference source locale
     * @return validation report
     */
    public LocaleValidationReport validateDirectory(Path directory, LocaleRef sourceLocale) {
        Objects.requireNonNull(directory, "directory cannot be null");
        Objects.requireNonNull(sourceLocale, "sourceLocale cannot be null");

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return new LocaleValidationReport(
                    sourceLocale,
                    0,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    List.of("Directory does not exist: " + directory)
            );
        }

        Map<LocaleRef, Map<String, String>> loaded = new HashMap<>();
        List<String> syntaxErrors = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.{yml,yaml}")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                try {
                    LocaleRef localeRef = LocaleRef.parse(baseName);
                    try (InputStream in = Files.newInputStream(path)) {
                        Map<String, String> messages = loader.load(in, fileName);
                        loaded.put(localeRef, messages);
                    }
                } catch (Exception e) {
                    syntaxErrors.add("Failed to parse " + fileName + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            syntaxErrors.add("I/O error reading directory " + directory + ": " + e.getMessage());
        }

        if (!syntaxErrors.isEmpty()) {
            return new LocaleValidationReport(
                    sourceLocale,
                    0,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    syntaxErrors
            );
        }

        return validate(sourceLocale, loaded);
    }

    private Set<String> findEmptyValues(Map<String, String> messages) {
        Set<String> empty = new TreeSet<>();
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                empty.add(entry.getKey());
            }
        }
        return empty;
    }
}
