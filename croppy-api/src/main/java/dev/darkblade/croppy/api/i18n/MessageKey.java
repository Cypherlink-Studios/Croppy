package dev.darkblade.croppy.api.i18n;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable semantic identifier for a translatable message template.
 */
public record MessageKey(String namespace, String key) implements Comparable<MessageKey> {

    public static final String DEFAULT_NAMESPACE = "croppy";
    private static final Pattern VALID_CHARS = Pattern.compile("^[a-z0-9_.-]+$");

    public MessageKey {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("MessageKey namespace cannot be null or blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("MessageKey key cannot be null or blank");
        }
        namespace = namespace.trim().toLowerCase();
        key = key.trim().toLowerCase();
        if (!VALID_CHARS.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid namespace characters in MessageKey: " + namespace);
        }
        if (!VALID_CHARS.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid key characters in MessageKey: " + key);
        }
    }

    public static MessageKey of(String namespace, String key) {
        return new MessageKey(namespace, key);
    }

    public static MessageKey of(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        int idx = key.indexOf(':');
        if (idx == -1) {
            return new MessageKey(DEFAULT_NAMESPACE, key);
        }
        return new MessageKey(key.substring(0, idx), key.substring(idx + 1));
    }

    public String asString() {
        return namespace + ":" + key;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public int compareTo(MessageKey o) {
        int nsComp = this.namespace.compareTo(o.namespace);
        return (nsComp != 0) ? nsComp : this.key.compareTo(o.key);
    }
}
