package dev.darkblade.croppy.api.station.component;

import dev.darkblade.croppy.api.identifier.Key;

import java.util.Optional;

/**
 * Component tracking the visual and display state of a station in the world.
 */
public interface DisplayComponent extends StationComponent {

    /**
     * @return current custom item key rendered as floating display icon
     */
    Optional<Key> getDisplayedItem();

    /**
     * Sets the custom item key to render.
     *
     * @param itemKey the item key, or null to clear
     */
    void setDisplayedItem(Key itemKey);

    /**
     * @return current status text (e.g. progress bar, recipe name), or empty string
     */
    String getStatusText();

    /**
     * Sets the status text to render.
     *
     * @param text status text
     */
    void setStatusText(String text);

    /**
     * @return current process progress fraction between 0.0f and 1.0f
     */
    default float getProgress() {
        return 0.0f;
    }

    /**
     * Sets the process progress fraction.
     *
     * @param progress value from 0.0f to 1.0f
     */
    default void setProgress(float progress) {}

    /**
     * @return current visual thermal zone name (e.g. "NONE", "WARM", "BOIL", "SEAR")
     */
    default String getThermalZone() {
        return "NONE";
    }

    /**
     * Sets the current visual thermal zone name.
     *
     * @param zone zone identifier
     */
    default void setThermalZone(String zone) {}

    /**
     * @return true if visual display entities require a synchronization update
     */
    boolean isDirty();

    /**
     * Sets dirty synchronization flag.
     *
     * @param dirty whether update is needed
     */
    void setDirty(boolean dirty);
}
