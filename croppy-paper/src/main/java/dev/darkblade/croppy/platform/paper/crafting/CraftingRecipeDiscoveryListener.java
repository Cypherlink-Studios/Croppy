package dev.darkblade.croppy.platform.paper.crafting;

import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.Set;

/**
 * Listens for player joins to optionally grant all registered Croppy crafting
 * recipes to the player's vanilla recipe book.
 */
public class CraftingRecipeDiscoveryListener implements Listener {

    private final PaperCraftingRecipeService recipeService;
    private final PluginConfig pluginConfig;

    public CraftingRecipeDiscoveryListener(PaperCraftingRecipeService recipeService, PluginConfig pluginConfig) {
        this.recipeService = Objects.requireNonNull(recipeService, "recipeService cannot be null");
        this.pluginConfig = Objects.requireNonNull(pluginConfig, "pluginConfig cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!pluginConfig.isCraftingAutoDiscoverRecipes()) {
            return;
        }

        Player player = event.getPlayer();
        Set<NamespacedKey> keys = recipeService.getRegisteredKeys();
        if (!keys.isEmpty()) {
            player.discoverRecipes(keys);
        }
    }
}
