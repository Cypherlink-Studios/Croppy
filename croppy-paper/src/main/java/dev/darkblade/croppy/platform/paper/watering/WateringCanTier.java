package dev.darkblade.croppy.platform.paper.watering;

import org.bukkit.Material;

import java.util.Locale;

/**
 * Tier configurations for player watering cans.
 */
public enum WateringCanTier {
    COPPER("copper", "<color:#e06666>Regadera de Cobre</color>", 30, 1, 1, Material.COPPER_INGOT, 2001),
    IRON("iron", "<white>Regadera de Hierro</white>", 80, 1, 3, Material.IRON_INGOT, 2002),
    GOLD("gold", "<gold>Regadera de Oro</gold>", 150, 3, 3, Material.GOLD_INGOT, 2003),
    DIAMOND("diamond", "<aqua>Regadera de Diamante</aqua>", 300, 3, 3, Material.DIAMOND, 2004),
    NETHERITE("netherite", "<color:#994433>Regadera de Netherite</color>", 600, 5, 5, Material.NETHERITE_INGOT, 2005);

    private final String id;
    private final String displayName;
    private final int capacity;
    private final int aoeWidth;
    private final int aoeLength;
    private final Material material;
    private final int customModelData;

    WateringCanTier(String id, String displayName, int capacity, int aoeWidth, int aoeLength, Material material, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.capacity = capacity;
        this.aoeWidth = aoeWidth;
        this.aoeLength = aoeLength;
        this.material = material;
        this.customModelData = customModelData;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int capacity() {
        return capacity;
    }

    public int aoeWidth() {
        return aoeWidth;
    }

    public int aoeLength() {
        return aoeLength;
    }

    public Material material() {
        return material;
    }

    public int customModelData() {
        return customModelData;
    }

    public static WateringCanTier parse(String input) {
        if (input == null || input.isBlank()) return COPPER;
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        for (WateringCanTier tier : values()) {
            if (tier.id.equalsIgnoreCase(normalized) || tier.name().equalsIgnoreCase(normalized)) {
                return tier;
            }
        }
        return COPPER;
    }
}
