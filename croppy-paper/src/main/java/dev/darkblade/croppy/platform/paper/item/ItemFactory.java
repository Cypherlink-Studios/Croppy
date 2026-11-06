package dev.darkblade.croppy.platform.paper.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.station.StoredItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Platform service responsible for constructing and inspecting Bukkit {@link ItemStack}s
 * backed by {@link ItemDefinition}s with standardized PersistentDataContainer tagging.
 */
public class ItemFactory {

    public static final String ITEM_ID_KEY_NAME = "item_id";
    public static final String CATEGORY_KEY_NAME = "category";

    private final Plugin plugin;
    private final Server server;
    private final ItemRegistry itemRegistry;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey categoryKey;
    private final MiniMessage miniMessage;
    private final Logger logger;

    public ItemFactory(Plugin plugin, ItemRegistry itemRegistry) {
        this(plugin, (plugin != null ? plugin.getServer() : null), itemRegistry);
    }

    public ItemFactory(Plugin plugin, Server server, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.server = server;
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry cannot be null");
        this.itemIdKey = new NamespacedKey("croppy", ITEM_ID_KEY_NAME);
        this.categoryKey = new NamespacedKey("croppy", CATEGORY_KEY_NAME);
        this.miniMessage = MiniMessage.miniMessage();
        this.logger = (plugin != null) ? plugin.getLogger() : Logger.getLogger(ItemFactory.class.getName());
    }

    /**
     * Constructs a Bukkit ItemStack from the given ItemDefinition.
     *
     * @param definition the domain item definition
     * @param amount number of items in the stack
     * @return the constructed ItemStack
     */
    public ItemStack createItemStack(ItemDefinition definition, int amount) {
        Objects.requireNonNull(definition, "definition cannot be null");

        Material material = resolveMaterial(definition.materialKey());
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        applyToMeta(meta, definition);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Applies metadata from an ItemDefinition to an existing ItemMeta instance.
     *
     * @param meta the item meta to configure
     * @param definition the domain item definition
     */
    public void applyToMeta(ItemMeta meta, ItemDefinition definition) {
        Objects.requireNonNull(meta, "meta cannot be null");
        Objects.requireNonNull(definition, "definition cannot be null");

        // 1. Display Name
        if (definition.displayName() != null && !definition.displayName().isBlank()) {
            Component name = miniMessage.deserialize(definition.displayName())
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);
        }

        // 2. Lore
        if (definition.lore() != null && !definition.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : definition.lore()) {
                lore.add(miniMessage.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }

        // 3. Custom Model Data & Native Item Model (Minecraft 1.21.4+)
        if (definition.customModelData() > 0) {
            meta.setCustomModelData(definition.customModelData());
        }
        if (definition.itemModel() != null) {
            meta.setItemModel(new NamespacedKey(definition.itemModel().namespace(), definition.itemModel().value()));
        }

        // 3b. Custom Skull Texture (PlayerProfile)
        if (meta instanceof SkullMeta skullMeta && definition.getSkullTexture().isPresent()) {
            PlayerProfile profile = createProfileFromTexture(definition.getSkullTexture().get());
            if (profile != null) {
                skullMeta.setPlayerProfile(profile);
            }
        }

        // 4. Standardized PersistentDataContainer
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc != null) {
            pdc.set(itemIdKey, PersistentDataType.STRING, definition.key().asString());
            pdc.set(categoryKey, PersistentDataType.STRING, definition.category());

            // 5. Arbitrary YAML PDC properties
            for (Map.Entry<String, String> entry : definition.pdcProperties().entrySet()) {
                NamespacedKey key = parseOrMakeKey(entry.getKey());
                pdc.set(key, PersistentDataType.STRING, entry.getValue());
            }
        }
    }

    public ItemStack createItemStack(ItemDefinition definition) {
        return createItemStack(definition, 1);
    }

    /**
     * Constructs a Bukkit ItemStack from a domain StoredItem.
     * If the item is registered in ItemRegistry, it builds the custom item.
     * If it is a vanilla item (unregistered), it returns an unmodified vanilla ItemStack
     * without custom display names or PDC tags to preserve item stacking compatibility.
     *
     * @param stored the domain stored item
     * @return the constructed Bukkit ItemStack
     */
    public ItemStack createBukkitItem(StoredItem stored) {
        Objects.requireNonNull(stored, "stored cannot be null");
        Optional<ItemDefinition> defOpt = itemRegistry.get(stored.itemKey());
        if (defOpt.isPresent()) {
            return createItemStack(defOpt.get(), stored.amount());
        }

        // Vanilla item fallback: untouched ItemStack
        Material mat = resolveMaterial(stored.itemKey().value());
        if (mat == Material.BARRIER && !stored.itemKey().value().equalsIgnoreCase("BARRIER")) {
            mat = resolveMaterial(stored.itemKey().asString());
        }
        return createRawVanillaItemStack(mat, Math.max(1, stored.amount()));
    }

    public ItemStack createRawVanillaItemStack(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    /**
     * Resolves an arbitrary item identifier (such as "croppy:grapes", "minecraft:wheat", or "wheat")
     * into a Bukkit ItemStack with amount 1.
     *
     * @param identifier the item identifier string
     * @return an Optional containing the constructed ItemStack, or empty if unresolvable
     */
    public Optional<ItemStack> resolveItemStack(String identifier) {
        return resolveItemStack(identifier, 1);
    }

    /**
     * Resolves an arbitrary item identifier (such as "croppy:grapes", "minecraft:wheat", or "wheat")
     * into a Bukkit ItemStack with the specified amount.
     *
     * <p>If the identifier corresponds to a registered {@link ItemDefinition} in {@link ItemRegistry},
     * the custom item is constructed with complete PDC metadata, display names, lore, and models.
     * If the identifier corresponds to a vanilla Minecraft material, a raw vanilla {@link ItemStack}
     * is created. If unresolvable or invalid, {@link Optional#empty()} is returned.</p>
     *
     * @param identifier the item identifier string
     * @param amount number of items in the stack (must be > 0)
     * @return an Optional containing the constructed ItemStack, or empty if unresolvable
     */
    public Optional<ItemStack> resolveItemStack(String identifier, int amount) {
        if (identifier == null || identifier.isBlank() || amount <= 0) {
            return Optional.empty();
        }

        String trimmed = identifier.trim();

        // 1. Check ItemRegistry by Key
        try {
            Key key = Key.parse(trimmed);
            Optional<ItemDefinition> defOpt = itemRegistry.get(key);
            if (defOpt.isPresent()) {
                return Optional.ofNullable(createItemStack(defOpt.get(), amount));
            }
        } catch (Exception ignored) {
        }

        // 2. Check vanilla Material
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        Material mat = Material.matchMaterial(normalized);
        if (mat != null && !isAir(mat)) {
            return Optional.ofNullable(createRawVanillaItemStack(mat, amount));
        }

        logger.warning("Unable to resolve item identifier '" + identifier + "' to an ItemRegistry entry or valid Material.");
        return Optional.empty();
    }

    private boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    /**
     * Resolves a raw texture string into a base64 encoded texture payload.
     * If a Mojang URL or 64-char hash is passed, it encapsulates it in a skin JSON payload.
     */
    public String resolveBase64Texture(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + trimmed + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        if (trimmed.length() == 64 && trimmed.matches("[0-9a-fA-F]+")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + trimmed + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return trimmed;
    }

    /**
     * Creates a PlayerProfile with a deterministic UUID for skin caching.
     */
    public PlayerProfile createProfileFromTexture(String rawTexture) {
        String base64 = resolveBase64Texture(rawTexture);
        if (base64 == null) {
            return null;
        }
        UUID uuid = UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8));
        Server s = (server != null) ? server : Bukkit.getServer();
        if (s == null) {
            return null;
        }
        PlayerProfile profile = s.createProfile(uuid, "");
        profile.setProperty(new ProfileProperty("textures", base64));
        return profile;
    }

    /**
     * Resolves the item ID key from the item's PDC.
     */
    public Optional<Key> getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String idStr = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (idStr == null || idStr.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Key.parse(idStr));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Resolves the category from the item's PDC.
     */
    public Optional<String> getCategory(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING));
    }

    /**
     * Checks if the ItemStack matches a specific Key.
     */
    public boolean isItem(ItemStack item, Key key) {
        return getItemId(item).map(k -> k.equals(key)).orElse(false);
    }

    /**
     * Checks if the ItemStack belongs to a specific category.
     */
    public boolean isCategory(ItemStack item, String category) {
        return getCategory(item).map(c -> c.equalsIgnoreCase(category)).orElse(false);
    }

    public NamespacedKey getItemIdKey() {
        return itemIdKey;
    }

    public NamespacedKey getCategoryKey() {
        return categoryKey;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    private Material resolveMaterial(String rawMaterial) {
        if (rawMaterial == null || rawMaterial.isBlank()) {
            return Material.BARRIER;
        }
        String normalized = rawMaterial.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        Material mat = Material.matchMaterial(normalized);
        if (mat == null) {
            logger.warning("Unknown material '" + rawMaterial + "' in ItemDefinition, falling back to BARRIER.");
            return Material.BARRIER;
        }
        return mat;
    }

    private NamespacedKey parseOrMakeKey(String keyStr) {
        if (keyStr.contains(":")) {
            String[] parts = keyStr.split(":", 2);
            return new NamespacedKey(parts[0], parts[1]);
        }
        return new NamespacedKey("croppy", keyStr);
    }
}
