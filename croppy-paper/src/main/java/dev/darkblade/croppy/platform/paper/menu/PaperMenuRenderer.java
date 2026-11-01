package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuItemTemplate;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.core.menu.MenuExpressionEvaluator;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for constructing Bukkit {@link Inventory} containers from {@link MenuDefinition}
 * layouts and synchronizing domain state.
 */
public class PaperMenuRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_.-]+)\\}");

    private final ItemFactory itemFactory;
    private final MiniMessage miniMessage;

    public PaperMenuRenderer(ItemFactory itemFactory) {
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory cannot be null");
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Constructs and populates a new Bukkit {@link Inventory} for the specified holder.
     *
     * @param holder menu holder
     * @return constructed and populated inventory
     */
    public Inventory createInventory(PaperMenuHolder holder) {
        Objects.requireNonNull(holder, "holder cannot be null");
        MenuDefinition menu = holder.getMenu();

        String rawTitle = replacePlaceholders(menu.title(), holder.getContext());
        Component titleComponent = miniMessage.deserialize(rawTitle).decoration(TextDecoration.ITALIC, false);
        Inventory inv = Bukkit.createInventory(holder, menu.size(), titleComponent);
        holder.setInventory(inv);

        // 1. Fill base static & conditional slots
        int totalSlots = menu.size();
        for (int i = 0; i < totalSlots; i++) {
            final int slotIndex = i;
            Optional<SlotDefinition> slotOpt = menu.getSlotAt(slotIndex);
            if (slotOpt.isEmpty()) {
                continue;
            }
            SlotDefinition slot = slotOpt.get();
            if (slot.type() == SlotType.DECORATION) {
                slot.getDefaultTemplate().ifPresent(t -> inv.setItem(slotIndex, renderTemplate(t, holder.getContext())));
            } else if (slot.type() == SlotType.ACTION || slot.type() == SlotType.STATUS) {
                Optional<MenuItemTemplate> template = MenuExpressionEvaluator.resolveTemplate(slot, holder.getContext());
                template.ifPresent(t -> inv.setItem(slotIndex, renderTemplate(t, holder.getContext())));
            }
        }

        // 2. Synchronize station storage contents if station is present
        if (holder.getStation().isPresent()) {
            populateStorageContents(holder);
        }

        return inv;
    }

    /**
     * Populates input and output slots in the menu inventory from the station's {@link StorageComponent}.
     *
     * @param holder the menu holder
     */
    public void populateStorageContents(PaperMenuHolder holder) {
        Optional<StationInstance> stationOpt = holder.getStation();
        if (stationOpt.isEmpty()) {
            return;
        }

        StationInstance station = stationOpt.get();
        Inventory inv = holder.getInventory();
        if (inv == null) {
            return;
        }

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return;
        }

        StorageComponent storage = storageOpt.get();
        MenuDefinition menu = holder.getMenu();

        // 1. Populate INPUT slots
        List<Integer> inputSlots = menu.getSlotsForType(SlotType.INPUT);
        List<StoredItem> inputs = storage.getInputs();
        for (int i = 0; i < inputSlots.size(); i++) {
            int slot = inputSlots.get(i);
            if (i < inputs.size()) {
                inv.setItem(slot, createBukkitItem(inputs.get(i)));
            } else {
                inv.setItem(slot, null);
            }
        }

        // 2. Populate OUTPUT slots
        List<Integer> outputSlots = menu.getSlotsForType(SlotType.OUTPUT);
        List<StoredItem> outputs = storage.getOutputs();
        for (int i = 0; i < outputSlots.size(); i++) {
            int slot = outputSlots.get(i);
            if (i < outputs.size()) {
                inv.setItem(slot, createBukkitItem(outputs.get(i)));
            } else {
                inv.setItem(slot, null);
            }
        }
    }

    /**
     * Refreshes dynamic status and action indicators in the menu without rebuilding the container.
     *
     * @param holder the menu holder
     */
    public void refreshDynamicSlots(PaperMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) {
            return;
        }

        MenuDefinition menu = holder.getMenu();

        // Refresh STATUS slots
        for (int slot : menu.getSlotsForType(SlotType.STATUS)) {
            menu.getSlotAt(slot).ifPresent(slotDef -> {
                Optional<MenuItemTemplate> template = MenuExpressionEvaluator.resolveTemplate(slotDef, holder.getContext());
                ItemStack item = template.map(t -> renderTemplate(t, holder.getContext())).orElse(null);
                inv.setItem(slot, item);
            });
        }

        // Refresh ACTION slots (in case conditional templates apply)
        for (int slot : menu.getSlotsForType(SlotType.ACTION)) {
            menu.getSlotAt(slot).ifPresent(slotDef -> {
                Optional<MenuItemTemplate> template = MenuExpressionEvaluator.resolveTemplate(slotDef, holder.getContext());
                ItemStack item = template.map(t -> renderTemplate(t, holder.getContext())).orElse(null);
                inv.setItem(slot, item);
            });
        }

        // Keep storage contents synchronized
        if (holder.getStation().isPresent()) {
            populateStorageContents(holder);
        }
    }

    /**
     * Renders an {@link ItemStack} from a {@link MenuItemTemplate} resolving MiniMessage formatting and placeholders.
     *
     * @param template template definition
     * @param context  evaluation context
     * @return constructed ItemStack
     */
    public ItemStack renderTemplate(MenuItemTemplate template, EvaluationContext context) {
        Objects.requireNonNull(template, "template cannot be null");
        Material mat = resolveMaterial(template.material());
        ItemStack item = new ItemStack(mat, template.amount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (template.displayName() != null && !template.displayName().isBlank()) {
            String resolved = replacePlaceholders(template.displayName(), context);
            meta.displayName(miniMessage.deserialize(resolved).decoration(TextDecoration.ITALIC, false));
        }

        if (!template.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : template.lore()) {
                String resolved = replacePlaceholders(line, context);
                lore.add(miniMessage.deserialize(resolved).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }

        if (template.customModelData() > 0) {
            meta.setCustomModelData(template.customModelData());
        }

        if (template.glow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an ItemStack representing a stored domain item.
     *
     * @param stored domain stored item
     * @return constructed Bukkit ItemStack
     */
    public ItemStack createBukkitItem(StoredItem stored) {
        Objects.requireNonNull(stored, "stored cannot be null");
        return itemFactory.getItemRegistry().get(stored.itemKey())
                .map(def -> itemFactory.createItemStack(def, stored.amount()))
                .orElseGet(() -> itemFactory.createItemStack(new ItemDefinition(
                        stored.itemKey(),
                        "misc",
                        stored.itemKey().asString(),
                        0,
                        "<gray>" + stored.itemKey().value() + "</gray>",
                        List.of(),
                        Map.of()
                ), stored.amount()));
    }

    /**
     * Resolves namespaced key or material string into a Bukkit {@link Material}.
     */
    public Material resolveMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return Material.BARRIER;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            normalized = normalized.substring("MINECRAFT:".length());
        }
        Material mat = Material.matchMaterial(normalized);
        return (mat != null) ? mat : Material.STONE;
    }

    /**
     * Replaces `{variable}` tokens with values from the {@link EvaluationContext}.
     */
    public static String replacePlaceholders(String text, EvaluationContext context) {
        if (text == null || text.isBlank() || context == null) {
            return (text == null) ? "" : text;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Optional<Object> val = context.getVariable(varName);
            String replacement = val.map(Object::toString).orElse("");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
