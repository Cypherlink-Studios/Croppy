package dev.darkblade.croppy.api.i18n;

import java.util.Optional;
import java.util.Set;

/**
 * Port interface for querying localized raw text templates and resolving message keys.
 */
public interface TranslationProvider {

    /**
     * Resolves the raw translation template for the given key and locale, applying fallback resolution.
     *
     * @param locale target locale
     * @param key    message key
     * @return raw template if resolved, or empty if not found
     */
    Optional<String> getTemplate(LocaleRef locale, MessageKey key);

    /**
     * Checks if a translation exists for the given key in the specified locale or fallback chain.
     *
     * @param locale target locale
     * @param key    message key
     * @return true if translatable
     */
    boolean hasTranslation(LocaleRef locale, MessageKey key);

    /**
     * Returns the set of all locales currently registered in this provider.
     *
     * @return unmodifiable set of registered locales
     */
    Set<LocaleRef> getSupportedLocales();

    /**
     * Returns the default server locale used as fallback.
     *
     * @return default locale reference
     */
    LocaleRef getDefaultLocale();
}
