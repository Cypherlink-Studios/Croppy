package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.menu.CompendiumMenuController;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/**
 * Listens for player right-clicks with the Culinary Compendium recipe book item
 * to open the culinary compendium cookbook interface.
 */
public class RecipeBookItemListener implements Listener {

    public static final Key RECIPE_BOOK_KEY = Key.of("croppy", "recipe_book");
    private static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("croppy", "item_id");
    private static final NamespacedKey ACTION_KEY = new NamespacedKey("croppy", "action");

    private final ItemFactory itemFactory;
    private final CompendiumMenuController compendiumController;

    public RecipeBookItemListener(ItemFactory itemFactory, CompendiumMenuController compendiumController) {
        this.itemFactory = itemFactory;
        this.compendiumController = Objects.requireNonNull(compendiumController, "compendiumController cannot be null");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isRecipeBook(item)) {
            return;
        }

        // Avoid duplicate triggers when holding the recipe book in both hands
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
            if (isRecipeBook(mainHand)) {
                return;
            }
        }

        event.setCancelled(true);
        openCompendium(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack item = (event.getHand() == EquipmentSlot.HAND)
                ? event.getPlayer().getInventory().getItemInMainHand()
                : event.getPlayer().getInventory().getItemInOffHand();

        if (!isRecipeBook(item)) {
            return;
        }

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
            if (isRecipeBook(mainHand)) {
                return;
            }
        }

        event.setCancelled(true);
        openCompendium(event.getPlayer());
    }

    /**
     * Determines whether the given item represents the Culinary Compendium recipe book.
     *
     * @param item the item stack to check
     * @return true if the item is a valid recipe book
     */
    public boolean isRecipeBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        if (itemFactory != null) {
            if (itemFactory.getItemId(item).filter(RECIPE_BOOK_KEY::equals).isPresent()) {
                return true;
            }
        }

        return isRecipeBook(item.getItemMeta());
    }

    /**
     * Determines whether the given item meta contains recipe book PDC tags.
     *
     * @param meta the item meta to check
     * @return true if the meta contains recipe book tags
     */
    public boolean isRecipeBook(ItemMeta meta) {
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String itemId = pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
        if ("croppy:recipe_book".equalsIgnoreCase(itemId)) {
            return true;
        }

        String action = pdc.get(ACTION_KEY, PersistentDataType.STRING);
        return "open_recipes".equalsIgnoreCase(action);
    }

    private void openCompendium(Player player) {
        try {
            player.playSound(player.getLocation(), "item.book.page_turn", 1.0f, 1.0f);
        } catch (Exception ignored) {
            // Decouple audio playback failures in test or headless environments
        }
        compendiumController.openCompendium(player, RecipeCategory.ALL.getKey(), 1);
    }
}
