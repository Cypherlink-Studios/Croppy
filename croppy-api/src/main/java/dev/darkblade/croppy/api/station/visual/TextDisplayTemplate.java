package dev.darkblade.croppy.api.station.visual;

/**
 * Configuration and format template for floating MiniMessage status text holograms.
 *
 * @param enabled         whether the text hologram is enabled
 * @param format          MiniMessage template string with placeholder tokens
 * @param transform       spatial translation, scale, and base rotation
 * @param billboard       camera orientation tracking mode
 * @param viewDistance    maximum viewing radius before culling
 * @param lineWidth       maximum line width in text pixels before wrapping
 * @param backgroundColor text background ARGB color (0 for default transparent)
 * @param seeThrough      whether text is visible through solid blocks
 * @param textShadow      whether text renders with a drop shadow
 * @param hideWhenIdle    whether the hologram is dynamically culled when the station is cold, empty, and idle
 */
public record TextDisplayTemplate(
        boolean enabled,
        String format,
        DisplayTransform transform,
        BillboardMode billboard,
        int viewDistance,
        int lineWidth,
        int backgroundColor,
        boolean seeThrough,
        boolean textShadow,
        boolean hideWhenIdle
) {

    public static final String DEFAULT_FORMAT = "<yellow><bold>{recipe_name}</bold></yellow>\n{progress_bar} <white>{progress_percent}%</white>";

    public TextDisplayTemplate {
        format = (format == null || format.isBlank()) ? DEFAULT_FORMAT : format;
        transform = (transform == null) ? DisplayTransform.of(0.0, 1.25, 0.0, 1.0f, 1.0f, 1.0f) : transform;
        billboard = (billboard == null) ? BillboardMode.CENTER : billboard;
        viewDistance = (viewDistance <= 0) ? 8 : viewDistance;
        lineWidth = (lineWidth <= 0) ? 200 : lineWidth;
    }

    public TextDisplayTemplate(
            boolean enabled,
            String format,
            DisplayTransform transform,
            BillboardMode billboard,
            int viewDistance,
            int lineWidth,
            int backgroundColor,
            boolean seeThrough,
            boolean textShadow
    ) {
        this(enabled, format, transform, billboard, viewDistance, lineWidth, backgroundColor, seeThrough, textShadow, false);
    }

    public static TextDisplayTemplate disabled() {
        return new TextDisplayTemplate(false, "", DisplayTransform.DEFAULT, BillboardMode.CENTER, 8, 200, 0, false, true, false);
    }
}
