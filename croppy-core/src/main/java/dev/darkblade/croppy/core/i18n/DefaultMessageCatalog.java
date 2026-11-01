package dev.darkblade.croppy.core.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.i18n.TranslationProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory message catalog implementing {@link TranslationProvider}
 * with atomic catalog reloading and a deterministic 5-tier fallback chain.
 */
public class DefaultMessageCatalog implements TranslationProvider {

    private final AtomicReference<Map<LocaleRef, Map<String, String>>> catalogsRef;
    private final Set<String> reportedMissingKeys;
    private volatile LocaleRef defaultLocale;
    private volatile LocaleRef sourceLocale;

    public DefaultMessageCatalog(LocaleRef defaultLocale, LocaleRef sourceLocale) {
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale cannot be null");
        this.sourceLocale = Objects.requireNonNull(sourceLocale, "sourceLocale cannot be null");
        this.catalogsRef = new AtomicReference<>(Collections.emptyMap());
        this.reportedMissingKeys = ConcurrentHashMap.newKeySet();
    }

    public DefaultMessageCatalog() {
        this(LocaleRef.EN_US, LocaleRef.EN_US);
    }

    /**
     * Atomically replaces the entire catalog map with a new immutable snapshot.
     *
     * @param newCatalogs map of locale to messages
     */
    public void setCatalogs(Map<LocaleRef, Map<String, String>> newCatalogs) {
        Objects.requireNonNull(newCatalogs, "newCatalogs cannot be null");
        Map<LocaleRef, Map<String, String>> copy = new HashMap<>();
        for (Map.Entry<LocaleRef, Map<String, String>> entry : newCatalogs.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        this.catalogsRef.set(Collections.unmodifiableMap(copy));
        this.reportedMissingKeys.clear();
    }

    /**
     * Adds or updates translations for a single locale atomically.
     *
     * @param locale   locale reference
     * @param messages messages map
     */
    public void registerLocale(LocaleRef locale, Map<String, String> messages) {
        Objects.requireNonNull(locale, "locale cannot be null");
        Objects.requireNonNull(messages, "messages cannot be null");

        catalogsRef.updateAndGet(current -> {
            Map<LocaleRef, Map<String, String>> updated = new HashMap<>(current);
            updated.put(locale, Map.copyOf(messages));
            return Collections.unmodifiableMap(updated);
        });
    }

    @Override
    public Optional<String> getTemplate(LocaleRef targetLocale, MessageKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        LocaleRef locale = (targetLocale != null) ? targetLocale : defaultLocale;
        Map<LocaleRef, Map<String, String>> catalogs = catalogsRef.get();

        // 1. Requested Locale
        String template = lookup(catalogs, locale, key);
        if (template != null) {
            return Optional.of(template);
        }

        // 2. Base Language of requested locale (e.g. es-AR -> es)
        if (locale.hasCountry()) {
            template = lookup(catalogs, locale.baseLanguage(), key);
            if (template != null) {
                return Optional.of(template);
            }
        }

        // 3. Server Default Locale
        if (!locale.equals(defaultLocale)) {
            template = lookup(catalogs, defaultLocale, key);
            if (template != null) {
                return Optional.of(template);
            }
            if (defaultLocale.hasCountry()) {
                template = lookup(catalogs, defaultLocale.baseLanguage(), key);
                if (template != null) {
                    return Optional.of(template);
                }
            }
        }

        // 4. Source Locale (en-US)
        if (!locale.equals(sourceLocale) && !defaultLocale.equals(sourceLocale)) {
            template = lookup(catalogs, sourceLocale, key);
            if (template != null) {
                return Optional.of(template);
            }
            if (sourceLocale.hasCountry()) {
                template = lookup(catalogs, sourceLocale.baseLanguage(), key);
                if (template != null) {
                    return Optional.of(template);
                }
            }
        }

        return Optional.empty();
    }

    private String lookup(Map<LocaleRef, Map<String, String>> catalogs, LocaleRef locale, MessageKey key) {
        Map<String, String> localeMap = catalogs.get(locale);
        if (localeMap == null) {
            return null;
        }
        // First check standard dot key (e.g. "command.item.given")
        String val = localeMap.get(key.key());
        if (val != null) {
            return val;
        }
        // If not found and namespace is non-standard, check full namespaced string
        if (!MessageKey.DEFAULT_NAMESPACE.equals(key.namespace())) {
            return localeMap.get(key.asString());
        }
        return null;
    }

    @Override
    public boolean hasTranslation(LocaleRef locale, MessageKey key) {
        return getTemplate(locale, key).isPresent();
    }

    @Override
    public Set<LocaleRef> getSupportedLocales() {
        return catalogsRef.get().keySet();
    }

    @Override
    public LocaleRef getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(LocaleRef defaultLocale) {
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale cannot be null");
    }

    public LocaleRef getSourceLocale() {
        return sourceLocale;
    }

    public void setSourceLocale(LocaleRef sourceLocale) {
        this.sourceLocale = Objects.requireNonNull(sourceLocale, "sourceLocale cannot be null");
    }

    /**
     * Records a missing key and returns true if this is the first time it has been reported.
     */
    public boolean markMissingKeyReported(String keyString) {
        return reportedMissingKeys.add(keyString);
    }

    public void clear() {
        catalogsRef.set(Collections.emptyMap());
        reportedMissingKeys.clear();
    }
}
