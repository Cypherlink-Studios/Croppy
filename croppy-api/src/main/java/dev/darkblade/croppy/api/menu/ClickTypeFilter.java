package dev.darkblade.croppy.api.menu;

/**
 * Filter specifying which player click types trigger a menu action.
 */
public enum ClickTypeFilter {

    /**
     * Matches any click type.
     */
    ALL,

    /**
     * Standard left click without modifiers.
     */
    LEFT,

    /**
     * Standard right click without modifiers.
     */
    RIGHT,

    /**
     * Shift-left click.
     */
    SHIFT_LEFT,

    /**
     * Shift-right click.
     */
    SHIFT_RIGHT,

    /**
     * Any shift-click variant (left or right).
     */
    SHIFT,

    /**
     * Middle mouse click (drop/pick).
     */
    MIDDLE;

    /**
     * Parses a string into a {@link ClickTypeFilter}, defaulting to {@link #ALL}.
     *
     * @param value raw string value
     * @return matching filter or ALL
     */
    public static ClickTypeFilter parse(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }
}
