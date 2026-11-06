package dev.darkblade.croppy.api.identifier;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable identifier for domain objects, decoupling Croppy from platform-specific keys.
 */
public record Key(String namespace, String value) implements Comparable<Key> {

    private static final Pattern VALID_NAMESPACE = Pattern.compile("^[a-z0-9_.-]+$");
    private static final Pattern VALID_VALUE = Pattern.compile("^[a-z0-9/._-]+$");
    public static final String DEFAULT_NAMESPACE = "croppy";

    public Key {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("Key namespace cannot be null or blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Key value cannot be null or blank");
        }
        namespace = namespace.trim().toLowerCase();
        value = value.trim().toLowerCase();
        if (!VALID_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid namespace characters: " + namespace);
        }
        if (!VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid value characters: " + value);
        }
    }

    public static Key of(String namespace, String value) {
        return new Key(namespace, value);
    }

    public static Key croppy(String value) {
        return new Key(DEFAULT_NAMESPACE, value);
    }

    public static Key parse(String formatted) {
        Objects.requireNonNull(formatted, "formatted string cannot be null");
        int index = formatted.indexOf(':');
        if (index == -1) {
            return new Key(DEFAULT_NAMESPACE, formatted);
        }
        return new Key(formatted.substring(0, index), formatted.substring(index + 1));
    }

    public String asString() {
        return namespace + ":" + value;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public int compareTo(Key o) {
        int nsComp = this.namespace.compareTo(o.namespace);
        return (nsComp != 0) ? nsComp : this.value.compareTo(o.value);
    }
}
