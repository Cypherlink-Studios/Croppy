package dev.darkblade.croppy.api.addon;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pure Java 21 configuration abstraction for Croppy addons.
 * Provides hierarchical dot-separated key querying, type-safe getters with fallbacks,
 * section navigation, runtime mutation, and persistence hooks.
 */
public interface AddonConfig {

    /**
     * @param path dot-separated key path
     * @return the string value or null if absent
     */
    String getString(String path);

    /**
     * @param path dot-separated key path
     * @param def fallback default value
     * @return the configured string or fallback
     */
    String getString(String path, String def);

    /**
     * @param path dot-separated key path
     * @return the integer value or 0 if absent
     */
    int getInt(String path);

    /**
     * @param path dot-separated key path
     * @param def fallback default value
     * @return the configured integer or fallback
     */
    int getInt(String path, int def);

    /**
     * @param path dot-separated key path
     * @return the long value or 0L if absent
     */
    long getLong(String path);

    /**
     * @param path dot-separated key path
     * @param def fallback default value
     * @return the configured long or fallback
     */
    long getLong(String path, long def);

    /**
     * @param path dot-separated key path
     * @return the double value or 0.0 if absent
     */
    double getDouble(String path);

    /**
     * @param path dot-separated key path
     * @param def fallback default value
     * @return the configured double or fallback
     */
    double getDouble(String path, double def);

    /**
     * @param path dot-separated key path
     * @return the boolean value or false if absent
     */
    boolean getBoolean(String path);

    /**
     * @param path dot-separated key path
     * @param def fallback default value
     * @return the configured boolean or fallback
     */
    boolean getBoolean(String path, boolean def);

    /**
     * @param path dot-separated key path
     * @return unmodifiable list of string values or empty list if absent
     */
    List<String> getStringList(String path);

    /**
     * @param path dot-separated key path
     * @return true if the path exists in this configuration
     */
    boolean contains(String path);

    /**
     * @param deep whether to recursively traverse nested sections
     * @return set of keys under the current root/section
     */
    Set<String> getKeys(boolean deep);

    /**
     * Navigates to a nested configuration section.
     *
     * @param path dot-separated path to the section
     * @return the nested AddonConfig section or empty if absent or not a section
     */
    Optional<AddonConfig> getSection(String path);

    /**
     * Sets or overwrites an in-memory configuration value.
     *
     * @param path dot-separated key path
     * @param value object value (null to remove)
     */
    void set(String path, Object value);

    /**
     * Reloads configuration from the underlying filesystem source.
     */
    void reload();

    /**
     * Persists in-memory configuration changes to the underlying file.
     */
    void save();
}
