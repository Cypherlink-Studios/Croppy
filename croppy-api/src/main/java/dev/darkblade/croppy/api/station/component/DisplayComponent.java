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
