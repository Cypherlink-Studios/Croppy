package dev.darkblade.croppy.platform.paper.listener;

import dev.darkblade.croppy.api.discovery.DiscoveryContext;
import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.storage.RecipeKnowledgeStorage;
import dev.darkblade.croppy.core.discovery.method.AutomaticDiscoveryMethod;
import dev.darkblade.croppy.platform.paper.discovery.DiscoveryFeedbackHelper;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bukkit event listener managing player recipe knowledge lifecycle (join/quit caching)
 * and triggering automatic recipe discovery upon item acquisition.
 */
public final class BukkitRecipeDiscoveryListener implements Listener {

    private final RecipeDiscoveryEngine discoveryEngine;
    private final RecipeRegistry recipeRegistry;
    private final RecipeKnowledgeStorage storage;
    private final dev.darkblade.croppy.platform.paper.discovery.DiscoveryFeedbackProvider feedbackHelper;
    private final Logger logger;

    private final NamespacedKey itemIdKey;
    private final AutomaticDiscoveryMethod automaticMethod = new AutomaticDiscoveryMethod();

    public BukkitRecipeDiscoveryListener(
            RecipeDiscoveryEngine discoveryEngine,
            RecipeRegistry recipeRegistry,
            RecipeKnowledgeStorage storage,
            dev.darkblade.croppy.platform.paper.discovery.DiscoveryFeedbackProvider feedbackHelper,
            Logger logger
    ) {
        this.discoveryEngine = Objects.requireNonNull(discoveryEngine, "discoveryEngine cannot be null");
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "recipeRegistry cannot be null");
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.feedbackHelper = Objects.requireNonNull(feedbackHelper, "feedbackHelper cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.itemIdKey = new NamespacedKey("croppy", "item_id");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        storage.loadKnowledge(player.getUniqueId()).thenAccept(knowledgeMap -> {
            discoveryEngine.loadPlayerKnowledge(player.getUniqueId(), knowledgeMap);
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to load recipe knowledge for player: " + player.getName(), ex);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        discoveryEngine.unloadPlayerKnowledge(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack stack = event.getItem().getItemStack();
            Key itemKey = resolveItemKey(stack);
            if (itemKey != null) {
                handleItemAcquired(player, itemKey);
            }
        }
    }

    /**
     * Evaluates automatic discovery triggers when a player acquires an item.
     *
     * @param player  player acquiring the item
     * @param itemKey key of the acquired item
     */
    public void handleItemAcquired(Player player, Key itemKey) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(itemKey, "itemKey cannot be null");

        DiscoveryContext context = DiscoveryContext.of(player.getUniqueId(), itemKey);

        for (RecipeDefinition recipe : recipeRegistry.values()) {
            if (!recipe.enabledDiscoveryMethods().contains(AutomaticDiscoveryMethod.KEY)) {
                continue;
            }

            if (discoveryEngine.getKnowledgeState(player.getUniqueId(), recipe.key()) == KnowledgeState.DISCOVERED) {
                continue;
            }

            DiscoveryResult evalResult = automaticMethod.evaluate(context, recipe);
            if (evalResult.isDiscovered()) {
                DiscoveryResult grantOutcome = discoveryEngine.discoverRecipe(player.getUniqueId(), recipe, AutomaticDiscoveryMethod.KEY);
                if (grantOutcome.isDiscovered()) {
                    discoveryEngine.getKnowledge(player.getUniqueId(), recipe.key())
                            .ifPresent(k -> storage.saveKnowledge(player.getUniqueId(), k));
                    feedbackHelper.playDiscoveryFanfare(player, recipe);
                }
            }
        }
    }

    private Key resolveItemKey(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String customId = pdc.get(itemIdKey, PersistentDataType.STRING);
            if (customId != null && !customId.isBlank()) {
                return Key.parse(customId);
            }
        }

        return Key.of("minecraft", stack.getType().name().toLowerCase(Locale.ROOT));
    }
}
