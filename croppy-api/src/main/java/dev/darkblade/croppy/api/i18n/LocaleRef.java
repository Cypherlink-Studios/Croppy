package dev.darkblade.croppy.api.i18n;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value representation of a normalized locale identifier (language and optional country/region).
 */
public record LocaleRef(String language, String country) implements Comparable<LocaleRef> {

    public static final LocaleRef EN_US = LocaleRef.of("en", "US");
    public static final LocaleRef ES_ES = LocaleRef.of("es", "ES");

    public LocaleRef {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("LocaleRef language cannot be null or blank");
        }
        language = language.trim().toLowerCase(Locale.ROOT);
        if (country != null) {
            country = country.trim().toUpperCase(Locale.ROOT);
            if (country.isEmpty()) {
                country = null;
            }
        }
    }

    public static LocaleRef of(String language, String country) {
        return new LocaleRef(language, country);
    }

    public static LocaleRef of(String language) {
        return new LocaleRef(language, null);
    }

    public static LocaleRef from(Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        return new LocaleRef(
                lang.isBlank() ? "en" : lang,
                country.isBlank() ? null : country
        );
    }

    public static LocaleRef parse(String tag) {
        Objects.requireNonNull(tag, "tag cannot be null");
        String trimmed = tag.trim().replace('_', '-');
        int dashIdx = trimmed.indexOf('-');
        if (dashIdx == -1) {
            return of(trimmed);
        }
        String lang = trimmed.substring(0, dashIdx);
        String country = trimmed.substring(dashIdx + 1);
        return of(lang, country);
    }

    public boolean hasCountry() {
        return country != null && !country.isEmpty();
    }

    public Optional<String> countryOptional() {
        return Optional.ofNullable(country);
    }

    public LocaleRef baseLanguage() {
        return hasCountry() ? of(language) : this;
    }

    public Locale toJavaLocale() {
        return (country != null) ? Locale.of(language, country) : Locale.of(language);
    }

    public String toLanguageTag() {
        return (country != null) ? (language + "-" + country) : language;
    }

    public String toUnderscoreTag() {
        return (country != null) ? (language + "_" + country) : language;
    }

    @Override
    public String toString() {
        return toLanguageTag();
    }

    @Override
    public int compareTo(LocaleRef o) {
        int langComp = this.language.compareTo(o.language);
        if (langComp != 0) {
            return langComp;
        }
        if (this.country == null && o.country == null) {
            return 0;
        }
        if (this.country == null) {
            return -1;
        }
        if (o.country == null) {
            return 1;
        }
        return this.country.compareTo(o.country);
    }
}
