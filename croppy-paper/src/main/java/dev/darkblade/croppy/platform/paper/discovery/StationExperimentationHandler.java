package dev.darkblade.croppy.platform.paper.discovery;

import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.recipe.KnowledgeState;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.storage.RecipeKnowledgeStorage;
import dev.darkblade.croppy.core.discovery.method.ExperimentalDiscoveryMethod;
import dev.darkblade.croppy.core.recipe.RecipeMatcher;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles experimental recipe discovery interactions inside crafting stations.
 * Evaluates inputs against station recipes, grants discoveries, and plays celebratory fanfare
 * without consuming any ingredients.
 */
public final class StationExperimentationHandler {

    private final RecipeRegistry recipeRegistry;
    private final RecipeDiscoveryEngine discoveryEngine;
    private final RecipeKnowledgeStorage storage;
    private final DiscoveryFeedbackProvider feedbackHelper;
    private final RecipeMatcher matcher;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public StationExperimentationHandler(
            RecipeRegistry recipeRegistry,
            RecipeDiscoveryEngine discoveryEngine,
            RecipeKnowledgeStorage storage,
            DiscoveryFeedbackProvider feedbackHelper
    ) {
        this.recipeRegistry = Objects.requireNonNull(recipeRegistry, "recipeRegistry cannot be null");
        this.discoveryEngine = Objects.requireNonNull(discoveryEngine, "discoveryEngine cannot be null");
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");
        this.feedbackHelper = Objects.requireNonNull(feedbackHelper, "feedbackHelper cannot be null");
        this.matcher = new RecipeMatcher();
    }

    /**
     * Evaluates the ingredients currently in the station for an experimental recipe discovery.
     *
     * @param player  the experimenting player
     * @param station the target station
     * @return true if a new recipe was successfully discovered, false otherwise
     */
    public boolean handleExperimentation(Player player, StationInstance station) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(station, "station cannot be null");

        Optional<StorageComponent> storageOpt = station.getComponent(StorageComponent.class);
        if (storageOpt.isEmpty()) {
            return false;
        }

        List<StoredItem> inputs = storageOpt.get().getInputs();
        if (inputs.isEmpty()) {
            player.sendMessage(miniMessage.deserialize("<gray>Place ingredients in the station before attempting experimentation.</gray>"));
            try {
                player.playSound(player.getLocation(), "block.dispenser.fail", 0.7f, 1.4f);
            } catch (Throwable ignored) {}
            return false;
        }

        Key stationKey = station.getStationKey();
        List<RecipeDefinition> candidates = recipeRegistry.findByStation(stationKey);

        Optional<RecipeDefinition> matchOpt = matcher.findMatch(candidates, inputs);
        if (matchOpt.isEmpty()) {
            player.sendMessage(miniMessage.deserialize("<gray>These ingredients don't seem to form any known culinary combination...</gray>"));
            try {
                player.playSound(player.getLocation(), "block.dispenser.fail", 0.7f, 1.4f);
            } catch (Throwable ignored) {}
            return false;
        }

        RecipeDefinition recipe = matchOpt.get();
        if (!recipe.enabledDiscoveryMethods().contains(ExperimentalDiscoveryMethod.KEY)) {
            player.sendMessage(miniMessage.deserialize("<gray>This preparation cannot be learned through basic kitchen experimentation.</gray>"));
            try {
                player.playSound(player.getLocation(), "block.dispenser.fail", 0.7f, 1.4f);
            } catch (Throwable ignored) {}
            return false;
        }

        KnowledgeState currentState = discoveryEngine.getKnowledgeState(player.getUniqueId(), recipe.key());
        if (currentState == KnowledgeState.DISCOVERED) {
            player.sendMessage(miniMessage.deserialize("<yellow>You already know this recipe: <white>" + recipe.displayName() + "</white>! You can start cooking.</yellow>"));
            try {
                player.playSound(player.getLocation(), "block.note_block.chime", 0.8f, 1.2f);
            } catch (Throwable ignored) {}
            return false;
        }

        DiscoveryResult result = discoveryEngine.discoverRecipe(player.getUniqueId(), recipe, ExperimentalDiscoveryMethod.KEY);
        if (result.isDiscovered()) {
            discoveryEngine.getKnowledge(player.getUniqueId(), recipe.key())
                    .ifPresent(k -> storage.saveKnowledge(player.getUniqueId(), k));
            feedbackHelper.playDiscoveryFanfare(player, recipe);
            return true;
        }

        return false;
    }
}
