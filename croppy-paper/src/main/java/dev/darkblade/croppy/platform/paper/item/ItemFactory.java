package dev.darkblade.croppy.platform.paper.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ConsumableProperties;
import dev.darkblade.croppy.api.item.ConsumeCooldown;
import dev.darkblade.croppy.api.item.ConsumeEffectSpec;
import dev.darkblade.croppy.api.item.FoodProperties;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemMetadataContributor;
import dev.darkblade.croppy.api.item.ItemMetadataRegistry;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.item.UseAnimation;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.visual.StationModelOverride;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.UseRemainder;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
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

/**
 * Platform service responsible for constructing and inspecting Bukkit {@link ItemStack}s
 * backed by {@link ItemDefinition}s with standardized PersistentDataContainer tagging.
 */
public class ItemFactory {

    public static final String ITEM_ID_KEY_NAME = "item_id";
    public static final String CATEGORY_KEY_NAME = "category";
    public static final String QUALITY_KEY_NAME = "quality";

    private final Plugin plugin;
    private final Server server;
    private final ItemRegistry itemRegistry;
    private final ItemMetadataRegistry metadataRegistry;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey qualityKey;
    private final MiniMessage miniMessage;
    private final CroppyLogger logger;
    private volatile boolean autoLoreEnabled = false;

    public ItemFactory(Plugin plugin, ItemRegistry itemRegistry) {
        this(plugin, (plugin != null ? plugin.getServer() : null), itemRegistry);
    }

    public ItemFactory(Plugin plugin, ItemRegistry itemRegistry, CroppyLogger logger) {
        this(plugin, (plugin != null ? plugin.getServer() : null), itemRegistry, null, logger);
    }

    public ItemFactory(Plugin plugin, ItemRegistry itemRegistry, ItemMetadataRegistry metadataRegistry, CroppyLogger logger) {
        this(plugin, (plugin != null ? plugin.getServer() : null), itemRegistry, metadataRegistry, logger);
    }

    public ItemFactory(Plugin plugin, Server server, ItemRegistry itemRegistry) {
        this(plugin, server, itemRegistry, null,
                (plugin != null && plugin.getLogger() != null)
                        ? new JulCroppyLogger(plugin.getLogger())
                        : NoOpCroppyLogger.INSTANCE);
    }

    public ItemFactory(Plugin plugin, Server server, ItemRegistry itemRegistry, CroppyLogger logger) {
        this(plugin, server, itemRegistry, null, logger);
    }

    public ItemFactory(Plugin plugin, Server server, ItemRegistry itemRegistry,
                       ItemMetadataRegistry metadataRegistry, CroppyLogger logger) {
        this.plugin = plugin;
        this.server = server;
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry cannot be null");
        this.metadataRegistry = metadataRegistry;
        this.itemIdKey = new NamespacedKey("croppy", ITEM_ID_KEY_NAME);
        this.categoryKey = new NamespacedKey("croppy", CATEGORY_KEY_NAME);
        this.qualityKey = new NamespacedKey("croppy", QUALITY_KEY_NAME);
        this.miniMessage = MiniMessage.miniMessage();
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
    }

    public void setAutoLoreEnabled(boolean autoLoreEnabled) {
        this.autoLoreEnabled = autoLoreEnabled;
    }

    public boolean isAutoLoreEnabled() {
        return autoLoreEnabled;
    }

    /**
     * Constructs a Bukkit ItemStack from the given ItemDefinition.
     *
     * @param definition the domain item definition
     * @param amount number of items in the stack
     * @return the constructed ItemStack
     */
    public ItemStack createItemStack(ItemDefinition definition, int amount) {
        return createItemStack(definition, amount, 0);
    }

    /**
     * Constructs a Bukkit ItemStack from the given ItemDefinition scaled for the given quality grade.
     */
    public ItemStack createItemStack(ItemDefinition definition, int amount, int quality) {
        Objects.requireNonNull(definition, "definition cannot be null");

        Material material = resolveMaterial(definition.materialKey());
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        applyToMeta(meta, definition, quality);

        item.setItemMeta(meta);
        applyConsumableDataComponents(item, definition, quality);
        return item;
    }

    /**
     * Applies metadata from an ItemDefinition to an existing ItemMeta instance.
     *
     * @param meta the item meta to configure
     * @param definition the domain item definition
     */
    public void applyToMeta(ItemMeta meta, ItemDefinition definition) {
        applyToMeta(meta, definition, 0);
    }

    /**
     * Applies metadata from an ItemDefinition to an existing ItemMeta instance for the specified quality grade.
     */
    public void applyToMeta(ItemMeta meta, ItemDefinition definition, int quality) {
        Objects.requireNonNull(meta, "meta cannot be null");
        Objects.requireNonNull(definition, "definition cannot be null");

        // 1. Display Name
        if (definition.displayName() != null && !definition.displayName().isBlank()) {
            Component name = miniMessage.deserialize(definition.displayName())
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);
        }

        // 2. Lore
        List<Component> lore = new ArrayList<>();
        if (definition.lore() != null && !definition.lore().isEmpty()) {
            for (String line : definition.lore()) {
                lore.add(miniMessage.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
        }
        if (autoLoreEnabled && definition.getConsumableProperties().isPresent()) {
            for (String autoLine : formatConsumableAutoLore(definition.getConsumableProperties().get(), quality)) {
                lore.add(miniMessage.deserialize(autoLine).decoration(TextDecoration.ITALIC, false));
            }
        }
        if (!lore.isEmpty()) {
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
        ItemStack item;
        Optional<ItemDefinition> defOpt = itemRegistry.get(stored.itemKey());
        if (defOpt.isPresent()) {
            item = createItemStack(defOpt.get(), stored.amount());
        } else {
            // Vanilla item fallback: untouched ItemStack unless quality or contributor metadata applies
            Material mat = resolveMaterial(stored.itemKey().value());
            if (mat == Material.BARRIER && !stored.itemKey().value().equalsIgnoreCase("BARRIER")) {
                mat = resolveMaterial(stored.itemKey().asString());
            }
            item = createRawVanillaItemStack(mat, Math.max(1, stored.amount()));
        }

        if (item == null) {
            return null;
        }
        applyStoredMetadata(item, stored);
        if (stored.quality() > 0 && defOpt.isPresent()) {
            applyConsumableDataComponents(item, defOpt.get(), stored.quality());
        }
        return item;
    }

    private void applyStoredMetadata(ItemStack item, StoredItem stored) {
        List<ItemMetadataContributor> contributors = matchingContributors(stored);
        if (stored.quality() <= 0 && contributors.isEmpty()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        applyStoredMetadataToMeta(meta, stored, contributors);
        item.setItemMeta(meta);
    }

    void applyStoredMetadataToMeta(ItemMeta meta, StoredItem stored) {
        applyStoredMetadataToMeta(meta, stored, matchingContributors(stored));
    }

    private List<ItemMetadataContributor> matchingContributors(StoredItem stored) {
        if (metadataRegistry == null || metadataRegistry.isEmpty()) {
            return List.of();
        }
        List<ItemMetadataContributor> matched = new ArrayList<>();
        for (ItemMetadataContributor contributor : metadataRegistry.values()) {
            if (contributor.supports(stored)) {
                matched.add(contributor);
            }
        }
        return matched;
    }

    private void applyStoredMetadataToMeta(ItemMeta meta, StoredItem stored, List<ItemMetadataContributor> contributors) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (stored.quality() > 0 && pdc != null) {
            pdc.set(qualityKey, PersistentDataType.INTEGER, stored.quality());
        }

        if (autoLoreEnabled && stored.quality() > 0) {
            Optional<ItemDefinition> defOpt = itemRegistry.get(stored.itemKey());
            if (defOpt.isPresent() && defOpt.get().getConsumableProperties().isPresent()) {
                List<Component> rebuilt = new ArrayList<>();
                for (String baseLine : defOpt.get().lore()) {
                    rebuilt.add(miniMessage.deserialize(baseLine).decoration(TextDecoration.ITALIC, false));
                }
                for (String autoLine : formatConsumableAutoLore(defOpt.get().getConsumableProperties().get(), stored.quality())) {
                    rebuilt.add(miniMessage.deserialize(autoLine).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(rebuilt);
            }
        }

        List<Component> extraLore = new ArrayList<>();
        for (ItemMetadataContributor contributor : contributors) {
            contributor.resolveDisplayName(stored).ifPresent(name ->
                    meta.displayName(miniMessage.deserialize(name).decoration(TextDecoration.ITALIC, false)));
            contributor.resolveItemModel(stored).ifPresent(modelKey ->
                    meta.setItemModel(new NamespacedKey(modelKey.namespace(), modelKey.value())));

            Map<String, String> tags = contributor.serializeMetadata(stored);
            if (tags != null && pdc != null) {
                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    pdc.set(parseOrMakeKey(entry.getKey()), PersistentDataType.STRING, entry.getValue());
                }
            }
            List<String> loreLines = contributor.formatLore(stored);
            if (loreLines != null) {
                for (String line : loreLines) {
                    extraLore.add(miniMessage.deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
            }
        }

        if (!extraLore.isEmpty()) {
            List<Component> currentLore = meta.lore();
            List<Component> combined = currentLore != null ? new ArrayList<>(currentLore) : new ArrayList<>();
            combined.addAll(extraLore);
            meta.lore(combined);
        }
    }

    /**
     * Reconstructs a domain {@link StoredItem} from a Bukkit {@link ItemStack}, preserving
     * quality and any metadata extracted by registered {@link ItemMetadataContributor}s.
     *
     * @param item the Bukkit ItemStack
     * @return optional containing the reconstructed StoredItem, or empty if null/air
     */
    public Optional<StoredItem> toStoredItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        Key itemKey = getItemId(item)
                .orElseGet(() -> Key.of("minecraft", item.getType().name().toLowerCase(Locale.ROOT)));

        int quality = 0;
        Map<String, Object> properties = new java.util.LinkedHashMap<>();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Integer storedQuality = pdc.get(qualityKey, PersistentDataType.INTEGER);
            if (storedQuality != null) {
                quality = storedQuality;
            }
            if (metadataRegistry != null && !metadataRegistry.isEmpty()) {
                Map<String, String> stringTags = new java.util.LinkedHashMap<>();
                for (NamespacedKey key : pdc.getKeys()) {
                    String val = pdc.get(key, PersistentDataType.STRING);
                    if (val != null) {
                        stringTags.put(key.namespace() + ":" + key.getKey(), val);
                    }
                }
                if (!stringTags.isEmpty()) {
                    for (ItemMetadataContributor contributor : metadataRegistry.values()) {
                        Map<String, Object> extracted = contributor.deserializeMetadata(itemKey, stringTags);
                        if (extracted != null && !extracted.isEmpty()) {
                            properties.putAll(extracted);
                        }
                    }
                }
            }
        }

        return Optional.of(new StoredItem(itemKey, item.getAmount(), quality, properties));
    }

    public ItemStack createRawVanillaItemStack(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    /**
     * Creates or updates an ItemStack applying the specified station model override
     * (item_model and/or custom_model_data).
     *
     * @param baseItem      the base ItemStack to copy or adapt
     * @param modelOverride the model override specification
     * @return a new or modified ItemStack with the model override applied
     */
    public ItemStack applyModelOverride(ItemStack baseItem, StationModelOverride modelOverride) {
        if (modelOverride == null) {
            return baseItem;
        }

        ItemStack item = baseItem;
        if (baseItem != null) {
            try {
                item = baseItem.clone();
            } catch (Throwable ignored) {
                item = baseItem;
            }
        } else {
            item = new ItemStack(Material.PAPER, 1);
        }

        ItemMeta meta = null;
        try {
            meta = item.getItemMeta();
        } catch (Throwable ignored) {}

        if (meta == null) {
            return item;
        }

        if (modelOverride.getItemModel().isPresent()) {
            Key imKey = modelOverride.getItemModel().get();
            meta.setItemModel(new NamespacedKey(imKey.namespace(), imKey.value()));
        }

        if (modelOverride.hasCustomModelData()) {
            meta.setCustomModelData(modelOverride.customModelData());
        }

        try {
            item.setItemMeta(meta);
        } catch (Throwable ignored) {}

        return item;
    }

    /**
     * Creates an ItemStack for a station base model, applying an optional state model override.
     *
     * @param baseItemKey   optional base item key
     * @param modelOverride optional state model override
     * @return constructed ItemStack for ItemDisplay
     */
    public ItemStack createStationModelItem(Key baseItemKey, StationModelOverride modelOverride) {
        ItemStack baseItem = null;
        if (baseItemKey != null) {
            Optional<ItemDefinition> defOpt = itemRegistry.get(baseItemKey);
            baseItem = defOpt.map(this::createItemStack).orElseGet(() -> {
                Material mat = resolveMaterial(baseItemKey.asString());
                return new ItemStack(mat != Material.AIR ? mat : Material.PAPER, 1);
            });
        }
        if (baseItem == null) {
            baseItem = new ItemStack(Material.PAPER, 1);
        }
        return applyModelOverride(baseItem, modelOverride);
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

        logger.warn(LogChannel.CORE, "Unable to resolve item identifier '" + identifier + "' to an ItemRegistry entry or valid Material.");
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
            logger.warn(LogChannel.CORE, "Unknown material '" + rawMaterial + "' in ItemDefinition, falling back to BARRIER.");
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

    /**
     * Applies native Paper 1.21.8 FOOD, CONSUMABLE (with empty vanilla consume-effects),
     * USE_REMAINDER, and USE_COOLDOWN Data Components when {@link ConsumableProperties} is present.
     */
    public void applyConsumableDataComponents(ItemStack item, ItemDefinition definition, int quality) {
        if (item == null || definition == null || definition.getConsumableProperties().isEmpty()) {
            return;
        }
        ConsumableProperties props = definition.getConsumableProperties().get();
        try {
            if (props.getFood().isPresent()) {
                FoodProperties fp = props.getFood().get();
                int scaledHunger = fp.qualityScaling().scaleHunger(fp.hunger(), quality);
                float scaledSat = fp.qualityScaling().scaleSaturation(fp.saturation(), quality);
                item.setData(DataComponentTypes.FOOD, io.papermc.paper.datacomponent.item.FoodProperties.food()
                        .nutrition(scaledHunger)
                        .saturation(scaledSat)
                        .canAlwaysEat(fp.canAlwaysEat())
                        .build());
            }

            Consumable.Builder consumableBuilder = Consumable.consumable()
                    .consumeSeconds(props.consumeSeconds())
                    .animation(mapAnimation(props.animation()))
                    .hasConsumeParticles(props.hasConsumeParticles())
                    .effects(List.of());

            if (props.getSound().isPresent()) {
                Key soundKey = props.getSound().get();
                consumableBuilder.sound(net.kyori.adventure.key.Key.key(soundKey.namespace(), soundKey.value()));
            }
            item.setData(DataComponentTypes.CONSUMABLE, consumableBuilder.build());

            if (props.getRemainderItemKey().isPresent()) {
                resolveItemStack(props.getRemainderItemKey().get().asString(), 1).ifPresent(rem ->
                        item.setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(rem)));
            }

            if (props.getCooldown().isPresent()) {
                ConsumeCooldown cd = props.getCooldown().get();
                Key groupKey = cd.getGroup().orElse(definition.key());
                item.setData(DataComponentTypes.USE_COOLDOWN,
                        UseCooldown.useCooldown(cd.seconds())
                                .cooldownGroup(net.kyori.adventure.key.Key.key(groupKey.namespace(), groupKey.value()))
                                .build());
            }
        } catch (Throwable t) {
            logger.debug(LogChannel.CORE, () ->
                    "Skipped DataComponent application for " + definition.key() + ": " + t.getMessage());
        }
    }

    /**
     * Formats optional MiniMessage auto-lore summary lines for consumable items.
     */
    public List<String> formatConsumableAutoLore(ConsumableProperties props, int quality) {
        if (props == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (props.getFood().isPresent()) {
            FoodProperties fp = props.getFood().get();
            int hunger = fp.qualityScaling().scaleHunger(fp.hunger(), quality);
            float sat = fp.qualityScaling().scaleSaturation(fp.saturation(), quality);
            lines.add(String.format(Locale.ROOT,
                    "<gray>Nutrition:</gray> <gold>%d</gold> <gray>| Saturation:</gray> <yellow>%.1f</yellow>",
                    hunger, sat));
        }
        for (ConsumeEffectSpec eff : props.effects()) {
            if (quality >= eff.minQuality()) {
                lines.add("<dark_gray>• " + eff.type().value().replace('_', ' ') + "</dark_gray>");
            }
        }
        return lines;
    }

    static ItemUseAnimation mapAnimation(UseAnimation animation) {
        if (animation == null) {
            return ItemUseAnimation.EAT;
        }
        return switch (animation) {
            case NONE -> ItemUseAnimation.NONE;
            case EAT -> ItemUseAnimation.EAT;
            case DRINK -> ItemUseAnimation.DRINK;
            case BLOCK -> ItemUseAnimation.BLOCK;
            case BOW -> ItemUseAnimation.BOW;
            case SPEAR -> ItemUseAnimation.SPEAR;
            case CROSSBOW -> ItemUseAnimation.CROSSBOW;
            case SPYGLASS -> ItemUseAnimation.SPYGLASS;
            case TOOT_HORN -> ItemUseAnimation.TOOT_HORN;
            case BRUSH -> ItemUseAnimation.BRUSH;
            case BUNDLE -> ItemUseAnimation.BUNDLE;
        };
    }
}
