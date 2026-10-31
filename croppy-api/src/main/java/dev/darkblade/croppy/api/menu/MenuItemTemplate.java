package dev.darkblade.croppy.api.menu;

import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of an item display template inside a menu slot.
 *
 * @param material        vanilla material identifier or custom Croppy item key (e.g., "minecraft:compass", "croppy:keg")
 * @param amount          stack amount (must be >= 1)
 * @param displayName     MiniMessage-formatted display name
 * @param lore            MiniMessage-formatted lore lines
 * @param customModelData custom model data integer
 * @param glow            whether to apply enchantment glint
 */
public record MenuItemTemplate(
        String material,
        int amount,
        String displayName,
        List<String> lore,
        int customModelData,
        boolean glow
) {

    public MenuItemTemplate {
        Objects.requireNonNull(material, "material cannot be null");
        if (amount < 1) {
            amount = 1;
        }
        displayName = (displayName == null) ? "" : displayName;
        lore = (lore == null) ? List.of() : List.copyOf(lore);
    }

    /**
     * Creates a simple menu item template with standard defaults.
     *
     * @param material vanilla material or custom item key
     * @param displayName MiniMessage display name
     * @return constructed template
     */
    public static MenuItemTemplate simple(String material, String displayName) {
        return new MenuItemTemplate(material, 1, displayName, List.of(), 0, false);
    }
}
