package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.discovery.DiscoveryResult;
import dev.darkblade.croppy.api.discovery.RecipeDiscoveryEngine;
import dev.darkblade.croppy.api.fertilizer.Fertilizer;
import dev.darkblade.croppy.api.fertilizer.FertilizerRegistry;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
import dev.darkblade.croppy.api.i18n.MessageKey;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.item.ItemCategory;
import dev.darkblade.croppy.api.item.ItemCategoryRegistry;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.item.ItemRegistry;
import dev.darkblade.croppy.api.recipe.RecipeCategory;
import dev.darkblade.croppy.api.recipe.RecipeCategoryRegistry;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.recipe.RecipeRegistry;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.storage.RecipeKnowledgeStorage;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.CroppyPlugin;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.fertilizer.FertilizerService;
import dev.darkblade.croppy.platform.paper.i18n.PaperMessageService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.logging.LogStyle;
import dev.darkblade.croppy.platform.paper.logging.PaperCroppyLogger;
import dev.darkblade.croppy.platform.paper.menu.CompendiumMenuController;
import dev.darkblade.croppy.platform.paper.menu.ItemCatalogController;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import dev.darkblade.croppy.platform.paper.service.CropTargetResolver;
import dev.darkblade.croppy.platform.paper.watering.WateringCanService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cloud 2.0 command controller handling all /croppy administrative subcommands.
 */
public class CroppyCommands {

    private final CroppyPlugin plugin;
    private final CropInspectorService inspectorService;
    private final PluginConfig pluginConfig;
    private final WateringCanService wateringCanService;
    private final FertilizerService fertilizerService;
    private final FertilizerRegistry fertilizerRegistry;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final PaperMessageService messageService;
    private final MiniMessage miniMessage;

    private RecipeRegistry recipeRegistry;
    private RecipeDiscoveryEngine discoveryEngine;
    private RecipeKnowledgeStorage recipeStorage;
    private CompendiumMenuController compendiumController;
    private RecipeCategoryRegistry recipeCategoryRegistry;
    private ItemCatalogController itemCatalogController;
    private ItemCategoryRegistry itemCategoryRegistry;
    private dev.darkblade.croppy.platform.paper.prompt.ChatPromptService chatPromptService;
    private dev.darkblade.croppy.platform.paper.addon.PaperAddonManager addonManager;
    private dev.darkblade.croppy.platform.paper.resourcepack.PaperResourcePackService resourcePackService;
    private dev.darkblade.croppy.api.update.UpdateService updateService;
    private CropTargetResolver targetResolver;
    private SpeciesRegistry speciesRegistry;
    private DisplaySynchronizer displaySynchronizer;
    private PaperCroppyLogger croppyLogger;

    public void setCroppyLogger(PaperCroppyLogger croppyLogger) {
        this.croppyLogger = croppyLogger;
    }

    private PaperCroppyLogger resolvePaperLogger() {
        if (this.croppyLogger != null) {
            return this.croppyLogger;
        }
        return this.plugin != null ? this.plugin.getCroppyLogger() : null;
    }

    private CroppyLogger resolveLogger() {
        PaperCroppyLogger resolved = resolvePaperLogger();
        return resolved != null ? resolved : NoOpCroppyLogger.INSTANCE;
    }

    public void setTargetResolver(CropTargetResolver targetResolver) {
        this.targetResolver = targetResolver;
    }

    public void setSpeciesRegistry(SpeciesRegistry speciesRegistry) {
        this.speciesRegistry = speciesRegistry;
    }

    public void setDisplaySynchronizer(DisplaySynchronizer displaySynchronizer) {
        this.displaySynchronizer = displaySynchronizer;
    }

    public CropTargetResolver getTargetResolver() {
        if (this.targetResolver != null) {
            return this.targetResolver;
        }
        if (plugin != null && plugin.getCore() != null) {
            this.targetResolver = new CropTargetResolver(
                    plugin.getCore().getCropManager(),
                    plugin.getVirtualBlockDisplayTracker(),
                    plugin.getCroppyLogger()
            );
            return this.targetResolver;
        }
        return null;
    }

    private SpeciesRegistry getSpeciesRegistry() {
        if (this.speciesRegistry != null) {
            return this.speciesRegistry;
        }
        if (plugin != null && plugin.getCore() != null) {
            return plugin.getCore().getSpeciesRegistry();
        }
        return null;
    }

    private DisplaySynchronizer getDisplaySynchronizer() {
        if (this.displaySynchronizer != null) {
            return this.displaySynchronizer;
        }
        if (plugin != null) {
            return plugin.getDisplaySynchronizer();
        }
        return null;
    }

    public void setChatPromptService(dev.darkblade.croppy.platform.paper.prompt.ChatPromptService chatPromptService) {
        this.chatPromptService = chatPromptService;
    }

    public void setItemCatalogController(ItemCatalogController itemCatalogController) {
        this.itemCatalogController = itemCatalogController;
    }

    public void setItemCategoryRegistry(ItemCategoryRegistry itemCategoryRegistry) {
        this.itemCategoryRegistry = itemCategoryRegistry;
    }

    public void setAddonManager(dev.darkblade.croppy.platform.paper.addon.PaperAddonManager addonManager) {
        this.addonManager = addonManager;
    }

    public void setResourcePackService(dev.darkblade.croppy.platform.paper.resourcepack.PaperResourcePackService resourcePackService) {
        this.resourcePackService = resourcePackService;
    }

    public void setUpdateService(dev.darkblade.croppy.api.update.UpdateService updateService) {
        this.updateService = updateService;
    }

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig,
            WateringCanService wateringCanService,
            FertilizerService fertilizerService,
            FertilizerRegistry fertilizerRegistry,
            ItemFactory itemFactory,
            ItemRegistry itemRegistry
    ) {
        this.plugin = plugin;
        this.inspectorService = Objects.requireNonNull(inspectorService, "inspectorService cannot be null");
        this.pluginConfig = Objects.requireNonNull(pluginConfig, "pluginConfig cannot be null");
        this.wateringCanService = wateringCanService;
        this.fertilizerService = fertilizerService;
        this.fertilizerRegistry = fertilizerRegistry;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        if (plugin != null && plugin.getMessageService() != null) {
            this.messageService = plugin.getMessageService();
        } else {
            dev.darkblade.croppy.core.i18n.DefaultMessageCatalog catalog = new dev.darkblade.croppy.core.i18n.DefaultMessageCatalog(dev.darkblade.croppy.api.i18n.LocaleRef.EN_US, dev.darkblade.croppy.api.i18n.LocaleRef.EN_US);
            dev.darkblade.croppy.core.i18n.YamlLocaleLoader loader = new dev.darkblade.croppy.core.i18n.YamlLocaleLoader();
            try {
                try (var enIn = CroppyCommands.class.getResourceAsStream("/locales/en-US.yml")) {
                    if (enIn != null) {
                        catalog.registerLocale(dev.darkblade.croppy.api.i18n.LocaleRef.EN_US, loader.load(enIn, "en-US.yml"));
                    }
                }
                try (var esIn = CroppyCommands.class.getResourceAsStream("/locales/es-ES.yml")) {
                    if (esIn != null) {
                        catalog.registerLocale(dev.darkblade.croppy.api.i18n.LocaleRef.ES_ES, loader.load(esIn, "es-ES.yml"));
                    }
                }
            } catch (Exception ignored) {}
            this.messageService = new PaperMessageService(catalog, dev.darkblade.croppy.api.logging.NoOpCroppyLogger.INSTANCE);
        }
        this.miniMessage = MiniMessage.miniMessage();
    }

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig
    ) {
        this(plugin, inspectorService, pluginConfig, null, null, null, null, null);
    }

    public CroppyCommands(
            CroppyPlugin plugin,
            CropInspectorService inspectorService,
            PluginConfig pluginConfig,
            WateringCanService wateringCanService,
            FertilizerService fertilizerService,
            FertilizerRegistry fertilizerRegistry,
            ItemFactory itemFactory,
            ItemRegistry itemRegistry,
            CropTargetResolver targetResolver
    ) {
        this(plugin, inspectorService, pluginConfig, wateringCanService, fertilizerService, fertilizerRegistry, itemFactory, itemRegistry);
        this.targetResolver = targetResolver;
    }

    private Optional<CropTargetResolver.TargetResult> resolveTargetCrop(Player player) {
        CropTargetResolver resolver = getTargetResolver();
        if (resolver != null) {
            return resolver.resolveTarget(player);
        }
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            return Optional.empty();
        }
        World world = target.getWorld();
        BlockPosition pos = BlockPosition.of(world.getName(), target.getX(), target.getY(), target.getZ());
        Optional<CropInstance> cropOpt = (plugin != null && plugin.getCore() != null)
                ? plugin.getCore().getCropManager().getCropAt(pos)
                : Optional.empty();
        if (cropOpt.isEmpty()) {
            BlockPosition above = BlockPosition.of(world.getName(), target.getX(), target.getY() + 1, target.getZ());
            cropOpt = (plugin != null && plugin.getCore() != null)
                    ? plugin.getCore().getCropManager().getCropAt(above)
                    : Optional.empty();
            if (cropOpt.isPresent()) {
                return Optional.of(new CropTargetResolver.TargetResult(above, cropOpt.get(), CropTargetResolver.TargetResult.TargetType.SUPPORTING_SOIL));
            }
            return Optional.empty();
        }
        return Optional.of(new CropTargetResolver.TargetResult(pos, cropOpt.get(), CropTargetResolver.TargetResult.TargetType.PHYSICAL_BLOCK));
    }

    public void setRecipeServices(
            RecipeRegistry recipeRegistry,
            RecipeDiscoveryEngine discoveryEngine,
            RecipeKnowledgeStorage recipeStorage,
            CompendiumMenuController compendiumController
    ) {
        setRecipeServices(recipeRegistry, discoveryEngine, recipeStorage, compendiumController, null);
    }

    public void setRecipeServices(
            RecipeRegistry recipeRegistry,
            RecipeDiscoveryEngine discoveryEngine,
            RecipeKnowledgeStorage recipeStorage,
            CompendiumMenuController compendiumController,
            RecipeCategoryRegistry recipeCategoryRegistry
    ) {
        this.recipeRegistry = recipeRegistry;
        this.discoveryEngine = discoveryEngine;
        this.recipeStorage = recipeStorage;
        this.compendiumController = compendiumController;
        this.recipeCategoryRegistry = recipeCategoryRegistry;
    }

    private void sendPrefixed(CommandSender sender, MessageKey key, Object... args) {
        if (messageService != null) {
            messageService.sendPrefixedMessage(sender, key, PaperMessageService.rawArgs(args), PaperMessageService.args(args));
        } else {
            sender.sendMessage(miniMessage.deserialize("<gradient:#70e000:#38b000><bold>Croppy</bold></gradient> <dark_gray>» <gray><red>" + key.key() + "</red>"));
        }
    }

    private void sendRaw(CommandSender sender, MessageKey key, Object... args) {
        if (messageService != null) {
            messageService.sendMessage(sender, key, PaperMessageService.rawArgs(args), PaperMessageService.args(args));
        } else {
            sender.sendMessage(miniMessage.deserialize(key.key()));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Dynamic Autocompletion Providers
    // ---------------------------------------------------------------------------------------------

    @Suggestions("item_categories")
    public List<String> suggestItemCategories(CommandContext<Source> context, CommandInput input) {
        if (itemRegistry == null) {
            return List.of();
        }
        return List.copyOf(itemRegistry.getCategories());
    }

    @Suggestions("category_items")
    public List<String> suggestCategoryItems(CommandContext<Source> context, CommandInput input) {
        if (itemRegistry == null) {
            return List.of();
        }
        String category = (context != null) ? context.getOrDefault("category", null) : null;
        if (category != null && !category.isBlank()) {
            return itemRegistry.getByCategory(category).stream()
                    .map(def -> def.key().asString())
                    .toList();
        }
        return itemRegistry.values().stream()
                .map(def -> def.key().asString())
                .toList();
    }

    @Suggestions("fertilizer_items")
    public List<String> suggestFertilizerItems(CommandContext<Source> context, CommandInput input) {
        if (fertilizerRegistry != null && !fertilizerRegistry.values().isEmpty()) {
            return fertilizerRegistry.values().stream()
                    .map(f -> f.getKey().asString())
                    .toList();
        }
        if (itemRegistry != null) {
            return itemRegistry.getByCategory("fertilizers").stream()
                    .map(def -> def.key().asString())
                    .toList();
        }
        return List.of();
    }

    @Suggestions("recipe_ids")
    public List<String> suggestRecipeIds(CommandContext<Source> context, CommandInput input) {
        if (recipeRegistry == null) {
            return List.of();
        }
        return recipeRegistry.values().stream()
                .map(r -> r.key().asString())
                .toList();
    }

    @Suggestions("compendium_actions")
    public List<String> suggestCompendiumActions(CommandContext<Source> context, CommandInput input) {
        List<String> actions = new ArrayList<>(List.of("all", "next", "prev", "back", "ready", "search", "clear"));
        if (recipeCategoryRegistry != null) {
            for (RecipeCategory cat : recipeCategoryRegistry.values()) {
                Key catKey = cat.getKey();
                if (!actions.contains(catKey.value())) {
                    actions.add(catKey.value());
                }
                if (!actions.contains(catKey.asString())) {
                    actions.add(catKey.asString());
                }
            }
        } else {
            actions.addAll(List.of("bakery", "soups", "cooking", "beverages", "preserves"));
        }
        return actions;
    }

    // ---------------------------------------------------------------------------------------------
    // Unified Item Commands
    // ---------------------------------------------------------------------------------------------

    @Command("croppy item give <category> <item_id> [quantity]")
    @Permission("croppy.admin")
    public void giveItem(
            Source source,
            @Argument(value = "category", suggestions = "item_categories") String category,
            @Argument(value = "item_id", suggestions = "category_items") Key itemId,
            @Argument("quantity") @Default("1") int quantity,
            @Flag("target") Player targetPlayer,
            @Flag("silent") boolean silent
    ) {
        CommandSender sender = source.source();
        Player recipient = targetPlayer;
        if (recipient == null) {
            if (sender instanceof Player p) {
                recipient = p;
            } else {
                sendPrefixed(sender, MessageKey.of("command.give.sender-required"));
                return;
            }
        }

        if (itemRegistry == null || itemFactory == null) {
            sendPrefixed(sender, MessageKey.of("command.give.registry-not-initialized"));
            return;
        }

        Optional<ItemDefinition> defOpt = itemRegistry.get(itemId);
        if (defOpt.isEmpty()) {
            sendPrefixed(sender, MessageKey.of("command.give.item-not-found"), "item", itemId.asString());
            return;
        }

        ItemDefinition def = defOpt.get();
        int amount = Math.max(1, quantity);
        ItemStack itemStack = itemFactory.createItemStack(def, amount);

        final Player recipientFinal = recipient;
        var leftover = recipientFinal.getInventory().addItem(itemStack);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(rem -> recipientFinal.getWorld().dropItemNaturally(recipientFinal.getLocation(), rem));
        }

        Component recipientItemDisplay = (messageService != null)
                ? messageService.resolveItemDisplayName(recipientFinal, def)
                : miniMessage.deserialize(def.displayName());

        if (!silent && !recipientFinal.equals(sender)) {
            sendPrefixed(recipientFinal, MessageKey.of("command.give.received"), "amount", amount, "item", recipientItemDisplay);
        }

        Component senderItemDisplay = (messageService != null)
                ? messageService.resolveItemDisplayName(sender, def)
                : miniMessage.deserialize(def.displayName());

        sendPrefixed(sender, MessageKey.of("command.give.given"), "amount", amount, "item", senderItemDisplay, "player", recipientFinal.getName());
    }

    @Command("croppy item list [category]")
    @Permission("croppy.admin")
    public void listItemDefinitions(
            Source source,
            @Argument(value = "category", suggestions = "item_categories") @Nullable String category
    ) {
        CommandSender sender = source.source();
        if (itemRegistry == null) {
            sendPrefixed(sender, MessageKey.of("command.give.registry-not-initialized"));
            return;
        }

        if (category != null && !category.isBlank()) {
            Collection<ItemDefinition> items = itemRegistry.getByCategory(category);
            if (items.isEmpty()) {
                sendPrefixed(sender, MessageKey.of("command.list.empty-category"), "category", category);
                return;
            }
            sendRaw(sender, MessageKey.of("command.list.category-header"), "category", category, "count", items.size());
            for (ItemDefinition def : items) {
                Component itemName = (messageService != null)
                        ? messageService.resolveItemDisplayName(sender, def)
                        : miniMessage.deserialize(def.displayName());
                sendRaw(sender, MessageKey.of("command.list.item-entry"),
                        "category", category,
                        "key", def.key().asString(),
                        "name", itemName);
            }
        } else {
            Collection<ItemDefinition> all = itemRegistry.values();
            sendRaw(sender, MessageKey.of("command.list.all-header"), "count", all.size());
            for (String cat : itemRegistry.getCategories()) {
                Collection<ItemDefinition> inCat = itemRegistry.getByCategory(cat);
                sender.sendMessage(miniMessage.deserialize("<aqua>[" + cat + "]</aqua> <gray>(" + inCat.size() + "):</gray>"));
                for (ItemDefinition def : inCat) {
                    Component itemName = (messageService != null)
                            ? messageService.resolveItemDisplayName(sender, def)
                            : miniMessage.deserialize(def.displayName());
                    sendRaw(sender, MessageKey.of("command.list.category-item-entry"),
                            "category", cat,
                            "key", def.key().asString(),
                            "name", itemName);
                }
            }
        }
    }

    @Command("croppy items [action] [query]")
    @Permission("croppy.admin.items")
    public void openItemsCatalog(
            Source source,
            @Argument(value = "action", suggestions = "catalog_actions") @Nullable String action,
            @Argument(value = "query") @Nullable @Greedy String query
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        if (itemCatalogController == null) {
            sendPrefixed(player, MessageKey.of("catalog.missing-config"));
            return;
        }

        if (action == null || action.isBlank() || "all".equalsIgnoreCase(action)) {
            itemCatalogController.openCatalog(player, ItemCategory.ALL.getKey(), 1);
            return;
        }

        if ("next".equalsIgnoreCase(action)) {
            itemCatalogController.nextPage(player);
            return;
        }

        if ("prev".equalsIgnoreCase(action) || "previous".equalsIgnoreCase(action)) {
            itemCatalogController.prevPage(player);
            return;
        }

        if ("catnext".equalsIgnoreCase(action)) {
            itemCatalogController.nextCategoryPage(player);
            return;
        }

        if ("catprev".equalsIgnoreCase(action) || "catprevious".equalsIgnoreCase(action)) {
            itemCatalogController.prevCategoryPage(player);
            return;
        }

        if ("clear".equalsIgnoreCase(action)) {
            if (chatPromptService != null) {
                chatPromptService.cancelPrompt(player.getUniqueId(), false);
            }
            itemCatalogController.clearFilters(player);
            sendPrefixed(player, MessageKey.of("catalog.filter.cleared"));
            return;
        }

        if ("search".equalsIgnoreCase(action)) {
            if (query == null || query.isBlank()) {
                itemCatalogController.startSearch(player);
            } else {
                String cleanQuery = query.trim();
                itemCatalogController.setSearchQuery(player, cleanQuery);
                sendPrefixed(player, MessageKey.of("catalog.filter.search"), "query", cleanQuery);
            }
            return;
        }

        // Check if action matches a registered category key or value
        Key catKey = action.contains(":") ? Key.parse(action) : Key.of("croppy", action.toLowerCase(Locale.ROOT));
        if (itemCategoryRegistry != null && itemCategoryRegistry.contains(catKey)) {
            itemCatalogController.openCatalog(player, catKey, 1);
            return;
        }

        // Fallback: search query
        itemCatalogController.setSearchQuery(player, action);
        sendPrefixed(player, MessageKey.of("catalog.filter.search"), "query", action);
    }

    @Suggestions("catalog_actions")
    public List<String> suggestCatalogActions(CommandContext<Source> context, CommandInput input) {
        List<String> list = new ArrayList<>();
        list.add("all");
        list.add("search");
        list.add("clear");
        list.add("next");
        list.add("prev");
        list.add("catnext");
        list.add("catprev");
        if (itemCategoryRegistry != null) {
            for (ItemCategory cat : itemCategoryRegistry.values()) {
                list.add(cat.key().value());
            }
        }
        return list;
    }

    // ---------------------------------------------------------------------------------------------
    // Core Crop & Inspection Commands
    // ---------------------------------------------------------------------------------------------

    @Command("croppy inspect")
    @Permission("croppy.admin")
    public void inspectRaycast(Source source) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.inspect.console-needs-coords"));
            return;
        }

        Optional<CropTargetResolver.TargetResult> targetOpt = resolveTargetCrop(player);
        if (targetOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        BlockPosition pos = targetOpt.get().cropPosition();
        Optional<Component> card = inspectorService.inspect(pos, player);
        if (card.isPresent()) {
            player.sendMessage(card.get());
        } else {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
        }
    }

    @Command("croppy inspect <x> <y> <z>")
    @Permission("croppy.admin")
    public void inspectCoordinates(
            Source source,
            @Argument("x") int x,
            @Argument("y") int y,
            @Argument("z") int z
    ) {
        CommandSender sender = source.source();
        World world = (sender instanceof Player player)
                ? player.getWorld()
                : (source.stack().getLocation().getWorld() != null ? source.stack().getLocation().getWorld() : Bukkit.getWorlds().get(0));
        BlockPosition pos = BlockPosition.of(world.getName(), x, y, z);
        Optional<Component> card = inspectorService.inspect(pos, sender);
        if (card.isPresent()) {
            sender.sendMessage(card.get());
        } else {
            sendPrefixed(sender, MessageKey.of("command.inspect.no-crop-found"));
        }
    }

    @Command("croppy reload")
    @Permission("croppy.admin")
    public void reload(Source source) {
        CommandSender sender = source.source();
        try {
            plugin.reloadPlugin();
            sendPrefixed(sender, MessageKey.of("general.reload-success"));
        } catch (Exception e) {
            sendPrefixed(sender, MessageKey.of("general.reload-failed"), "error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            resolveLogger().error(LogChannel.CORE, "Failed to reload Croppy", e);
        }
    }

    @Command("croppy water")
    @Permission("croppy.admin")
    public void waterTarget(Source source) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Optional<CropTargetResolver.TargetResult> targetOpt = resolveTargetCrop(player);
        if (targetOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        CropTargetResolver.TargetResult target = targetOpt.get();
        CropInstance crop = target.crop();
        BlockPosition pos = target.cropPosition();
        World world = player.getWorld();

        long currentDay = world.getFullTime() / 24000L;
        crop.setLastWateredDay(currentDay);

        Block blockBelow = world.getBlockAt(crop.getPosition().x(), crop.getPosition().y() - 1, crop.getPosition().z());
        if (blockBelow.getType() == Material.FARMLAND && blockBelow.getBlockData() instanceof Farmland farmland) {
            farmland.setMoisture(7);
            blockBelow.setBlockData(farmland, false);
        }

        Location particleLoc = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        world.spawnParticle(Particle.SPLASH, particleLoc, 15, 0.2, 0.2, 0.2, 0.1);
        player.playSound(particleLoc, Sound.ITEM_BUCKET_EMPTY, 0.8f, 1.2f);
        sendPrefixed(player, MessageKey.of("command.water.success"));
    }

    @Command("croppy fertilize <fertilizer>")
    @Permission("croppy.admin")
    public void fertilizeTarget(
            Source source,
            @Argument(value = "fertilizer", suggestions = "fertilizer_items") Key fertilizerKey
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Optional<CropTargetResolver.TargetResult> targetOpt = resolveTargetCrop(player);
        if (targetOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        CropTargetResolver.TargetResult target = targetOpt.get();
        CropInstance crop = target.crop();
        BlockPosition pos = target.cropPosition();
        World world = player.getWorld();

        crop.setFertilizerKey(fertilizerKey);

        Location particleLoc = new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        world.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 12, 0.2, 0.2, 0.2, 0.1);
        player.playSound(particleLoc, Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.1f);
        sendPrefixed(player, MessageKey.of("command.fertilize.success"), "fertilizer", fertilizerKey.asString());
    }

    @Command("croppy setstage <stageIndex>")
    @Permission("croppy.admin")
    public void setStage(
            Source source,
            @Argument("stageIndex") int stageIndex
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Optional<CropTargetResolver.TargetResult> targetOpt = resolveTargetCrop(player);
        if (targetOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        CropInstance crop = targetOpt.get().crop();
        SpeciesRegistry registry = getSpeciesRegistry();
        Optional<Species> speciesOpt = (registry != null) ? registry.get(crop.getSpeciesKey()) : Optional.empty();
        if (speciesOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.state.species-not-found"));
            return;
        }

        Species species = speciesOpt.get();
        if (stageIndex < 0 || stageIndex >= species.getTotalStages()) {
            sendPrefixed(player, MessageKey.of("command.state.invalid-stage"), "stage", stageIndex, "max", species.getTotalStages() - 1);
            return;
        }

        crop.setStageIndex(stageIndex);
        crop.setProgress(0.0);
        StageDefinition stage = species.getStage(stageIndex);
        DisplaySynchronizer synchronizer = getDisplaySynchronizer();
        if (synchronizer != null) {
            synchronizer.synchronizeDisplay(crop, species, stage);
        }

        sendPrefixed(player, MessageKey.of("command.state.stage-updated"), "stage", stageIndex);
    }

    @Command("croppy setprogress <progress>")
    @Permission("croppy.admin")
    public void setProgress(
            Source source,
            @Argument("progress") double progress
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        Optional<CropTargetResolver.TargetResult> targetOpt = resolveTargetCrop(player);
        if (targetOpt.isEmpty()) {
            sendPrefixed(player, MessageKey.of("command.inspect.no-crop-found"));
            return;
        }

        CropInstance crop = targetOpt.get().crop();
        crop.setProgress(Math.max(0.0, progress));

        sendPrefixed(player, MessageKey.of("command.state.progress-updated"), "progress", String.format(Locale.ROOT, "%.1f", crop.getProgress()));
    }

    @Command("croppy recipes [action] [query]")
    public void openRecipes(
            Source source,
            @Argument(value = "action", suggestions = "compendium_actions") @Nullable String action,
            @Argument(value = "query") @Nullable @Greedy String query
    ) {
        CommandSender sender = source.source();
        if (!(sender instanceof Player player)) {
            sendPrefixed(sender, MessageKey.of("command.player-only"));
            return;
        }

        if (compendiumController == null) {
            sendPrefixed(player, MessageKey.of("recipe.compendium_unavailable"));
            return;
        }

        if (action == null || action.isBlank() || "all".equalsIgnoreCase(action)) {
            compendiumController.openCompendium(player, RecipeCategory.ALL.getKey(), 1);
            return;
        }

        if ("next".equalsIgnoreCase(action)) {
            compendiumController.nextPage(player);
            return;
        }

        if ("prev".equalsIgnoreCase(action) || "previous".equalsIgnoreCase(action)) {
            compendiumController.prevPage(player);
            return;
        }

        if ("back".equalsIgnoreCase(action)) {
            compendiumController.backToCompendium(player);
            return;
        }

        if ("clear".equalsIgnoreCase(action)) {
            if (chatPromptService != null) {
                chatPromptService.cancelPrompt(player.getUniqueId(), false);
            }
            compendiumController.clearFilters(player);
            sendPrefixed(player, MessageKey.of("compendium.filter.ready-off"));
            return;
        }

        if ("ready".equalsIgnoreCase(action)) {
            compendiumController.toggleReadyFilter(player);
            boolean ready = compendiumController.getSession(player.getUniqueId())
                    .map(CompendiumMenuController.CompendiumSession::onlyReadyToCook)
                    .orElse(false);
            sendPrefixed(player, MessageKey.of(ready ? "compendium.filter.ready-on" : "compendium.filter.ready-off"));
            return;
        }

        if ("search".equalsIgnoreCase(action)) {
            if (query == null || query.isBlank()) {
                if (chatPromptService != null) {
                    chatPromptService.startSearchPrompt(
                            player,
                            q -> {
                                if (q == null || q.isBlank()) {
                                    compendiumController.setSearchQuery(player, null);
                                    sendPrefixed(player, MessageKey.of("compendium.filter.ready-off"));
                                } else {
                                    compendiumController.setSearchQuery(player, q);
                                    sendPrefixed(player, MessageKey.of("compendium.filter.search"), "query", q);
                                }
                            },
                            () -> compendiumController.backToCompendium(player)
                    );
                } else {
                    compendiumController.setSearchQuery(player, null);
                    sendPrefixed(player, MessageKey.of("compendium.filter.ready-off"));
                }
            } else {
                String cleanQuery = query.trim();
                compendiumController.setSearchQuery(player, cleanQuery);
                sendPrefixed(player, MessageKey.of("compendium.filter.search"), "query", cleanQuery);
            }
            return;
        }

        // Category resolution: match registered categories first
        Key catKey = null;
        if (recipeCategoryRegistry != null) {
            Optional<RecipeCategory> category = recipeCategoryRegistry.values().stream()
                    .filter(c -> c.getKey().value().equalsIgnoreCase(action) || c.getKey().asString().equalsIgnoreCase(action))
                    .findFirst();
            if (category.isPresent()) {
                catKey = category.get().getKey();
            }
        }

        if (catKey == null) {
            try {
                catKey = Key.parse(action);
            } catch (IllegalArgumentException e) {
                sendPrefixed(player, MessageKey.of("recipe.unknown_recipe"), "recipe", action);
                return;
            }
        }

        compendiumController.setCategory(player, catKey);
    }

    @Command("croppy recipe give <target> <recipe_id>")
    @Permission("croppy.admin")
    public void giveRecipe(
            Source source,
            @Argument("target") Player target,
            @Argument(value = "recipe_id", suggestions = "recipe_ids") Key recipeId
    ) {
        CommandSender sender = source.source();
        if (recipeRegistry == null || discoveryEngine == null) {
            return;
        }

        Optional<RecipeDefinition> recipeOpt = recipeRegistry.get(recipeId);
        if (recipeOpt.isEmpty()) {
            sendPrefixed(sender, MessageKey.of("recipe.unknown_recipe"), "recipe", recipeId.asString());
            return;
        }

        RecipeDefinition recipe = recipeOpt.get();
        DiscoveryResult result = discoveryEngine.discoverRecipe(target.getUniqueId(), recipe, Key.of("croppy", "admin"));
        if (result.isDiscovered()) {
            if (recipeStorage != null) {
                discoveryEngine.getKnowledge(target.getUniqueId(), recipe.key())
                        .ifPresent(k -> recipeStorage.saveKnowledge(target.getUniqueId(), k));
            }
            Component recipeName = (messageService != null)
                    ? messageService.resolveRecipeDisplayName(sender, recipe)
                    : miniMessage.deserialize(recipe.displayName());
            sendPrefixed(sender, MessageKey.of("recipe.admin_give_success"), "player", target.getName(), "recipe", recipeName);
            Component targetRecipeName = (messageService != null)
                    ? messageService.resolveRecipeDisplayName(target, recipe)
                    : miniMessage.deserialize(recipe.displayName());
            sendPrefixed(target, MessageKey.of("command.recipe.unlocked_by_admin"), "recipe", targetRecipeName);
        } else {
            Component recipeName = (messageService != null)
                    ? messageService.resolveRecipeDisplayName(sender, recipe)
                    : miniMessage.deserialize(recipe.displayName());
            sendPrefixed(sender, MessageKey.of("recipe.already_known"), "player", target.getName(), "recipe", recipeName);
        }
    }

    @Command("croppy recipe revoke <target> <recipe_id>")
    @Permission("croppy.admin")
    public void revokeRecipe(
            Source source,
            @Argument("target") Player target,
            @Argument(value = "recipe_id", suggestions = "recipe_ids") Key recipeId
    ) {
        CommandSender sender = source.source();
        if (discoveryEngine == null) {
            return;
        }

        boolean revoked = discoveryEngine.revokeRecipe(target.getUniqueId(), recipeId);
        if (revoked) {
            if (recipeStorage != null) {
                recipeStorage.deleteKnowledge(target.getUniqueId(), recipeId);
            }
            sendPrefixed(sender, MessageKey.of("recipe.admin_revoke_success"), "player", target.getName(), "recipe", recipeId.asString());
        } else {
            sendPrefixed(sender, MessageKey.of("recipe.not_known"), "player", target.getName(), "recipe", recipeId.asString());
        }
    }

    @Command("croppy recipe list")
    @Permission("croppy.admin")
    public void listRecipes(Source source) {
        CommandSender sender = source.source();
        if (recipeRegistry == null || recipeRegistry.values().isEmpty()) {
            sendPrefixed(sender, MessageKey.of("recipe.no_recipes"));
            return;
        }

        sendRaw(sender, MessageKey.of("command.recipe.header"), "count", recipeRegistry.size());
        for (RecipeDefinition r : recipeRegistry.values()) {
            Component recipeName = (messageService != null)
                    ? messageService.resolveRecipeDisplayName(sender, r)
                    : miniMessage.deserialize(r.displayName());
            sendRaw(sender, MessageKey.of("command.recipe.entry"),
                    "key", r.key().asString(),
                    "recipe", recipeName,
                    "category", r.categoryKey().value(),
                    "station", r.stationKey().value());
        }
    }

    @Suggestions("addon_ids")
    public List<String> suggestAddonIds(CommandContext<Source> context, CommandInput input) {
        if (addonManager == null) {
            return List.of("all");
        }
        List<String> list = new java.util.ArrayList<>();
        list.add("all");
        for (dev.darkblade.croppy.api.addon.AddonDescription desc : addonManager.getDiscoveredAddons()) {
            list.add(desc.id());
        }
        return list;
    }

    @Command("croppy addon list")
    @Permission("croppy.admin.addon")
    public void listAddons(Source source) {
        CommandSender sender = source.source();
        if (addonManager == null || addonManager.getDiscoveredAddons().isEmpty()) {
            sendPrefixed(sender, MessageKey.of("command.addon.no-addons"));
            return;
        }

        sendRaw(sender, MessageKey.of("command.addon.header"), "count", addonManager.getDiscoveredAddons().size());
        for (dev.darkblade.croppy.api.addon.AddonDescription addon : addonManager.getDiscoveredAddons()) {
            dev.darkblade.croppy.api.addon.AddonState state = addonManager.getState(addon.id());
            String stateColor = switch (state) {
                case ENABLED -> "<green>ENABLED</green>";
                case LOADED -> "<yellow>LOADED</yellow>";
                case ON_DISK -> "<gray>ON_DISK</gray>";
            };
            String type = addon.isCodeAddon() ? "Java" : "Content Pack";
            sendRaw(sender, MessageKey.of("command.addon.entry"),
                    "id", addon.id(),
                    "name", addon.name(),
                    "version", addon.version(),
                    "type", type,
                    "status", miniMessage.deserialize(stateColor));
        }
    }

    @Command("croppy addon status <addonId>")
    @Permission("croppy.admin.addon")
    public void addonStatus(
            Source source,
            @Argument(value = "addonId", suggestions = "addon_ids") String addonId
    ) {
        CommandSender sender = source.source();
        if (addonManager == null) return;

        Optional<dev.darkblade.croppy.api.addon.AddonDescription> descOpt = addonManager.getAddon(addonId);
        if (descOpt.isEmpty()) {
            sendPrefixed(sender, MessageKey.of("command.addon.not-found"), "addon", addonId);
            return;
        }

        dev.darkblade.croppy.api.addon.AddonDescription desc = descOpt.get();
        dev.darkblade.croppy.api.addon.AddonState state = addonManager.getState(addonId);
        String stateColor = switch (state) {
            case ENABLED -> "<green>ENABLED</green>";
            case LOADED -> "<yellow>LOADED</yellow>";
            case ON_DISK -> "<gray>ON_DISK</gray>";
        };

        sendRaw(sender, MessageKey.of("command.addon.info-header"), "name", desc.name(), "id", desc.id());
        sendRaw(sender, MessageKey.of("command.addon.info-version"), "version", desc.version());
        sendRaw(sender, MessageKey.of("command.addon.info-namespace"), "namespace", desc.namespace());
        sendRaw(sender, MessageKey.of("command.addon.info-type"), "type", desc.isCodeAddon() ? "Java Code Addon" : "Declarative Content Pack");
        sendRaw(sender, MessageKey.of("command.addon.info-state"), "status", miniMessage.deserialize(stateColor));
        if (!desc.authors().isEmpty()) {
            sendRaw(sender, MessageKey.of("command.addon.info-authors"), "authors", String.join(", ", desc.authors()));
        }
        if (!desc.dependencies().isEmpty()) {
            sendRaw(sender, MessageKey.of("command.addon.info-dependencies"), "dependencies", String.join(", ", desc.dependencies()));
        }
        if (state == dev.darkblade.croppy.api.addon.AddonState.ENABLED) {
            addonManager.getContext(addonId).ifPresent(ctx -> {
                sendRaw(sender, MessageKey.of("command.addon.info-processes"), "count", ctx.getRegisteredProcesses().size());
                sendRaw(sender, MessageKey.of("command.addon.info-listeners"), "count", ctx.getRegisteredListeners().size());
                sendRaw(sender, MessageKey.of("command.addon.info-menus"), "count", ctx.getRegisteredMenus().size());
                sendRaw(sender, MessageKey.of("command.addon.info-interactions"), "count", ctx.getRegisteredInteractions().size());
            });
        }
    }

    @Command("croppy addon load <addonId>")
    @Permission("croppy.admin.addon")
    public void loadAddon(
            Source source,
            @Argument(value = "addonId", suggestions = "addon_ids") String addonId
    ) {
        CommandSender sender = source.source();
        if (addonManager == null) return;

        if ("all".equalsIgnoreCase(addonId)) {
            addonManager.discoverAddons();
            int count = 0;
            for (dev.darkblade.croppy.api.addon.AddonDescription desc : addonManager.getDiscoveredAddons()) {
                if (addonManager.loadAddon(desc.id())) count++;
            }
            sendPrefixed(sender, MessageKey.of("command.addon.load-all-success"), "count", count);
            return;
        }

        if (addonManager.loadAddon(addonId)) {
            sendPrefixed(sender, MessageKey.of("command.addon.load-success"), "addon", addonId);
        } else {
            sendPrefixed(sender, MessageKey.of("command.addon.load-failed"), "addon", addonId);
        }
    }

    @Command("croppy addon enable <addonId>")
    @Permission("croppy.admin.addon")
    public void enableAddon(
            Source source,
            @Argument(value = "addonId", suggestions = "addon_ids") String addonId
    ) {
        CommandSender sender = source.source();
        if (addonManager == null) return;

        if ("all".equalsIgnoreCase(addonId)) {
            addonManager.enableAll();
            sendPrefixed(sender, MessageKey.of("command.addon.enable-all-success"));
            return;
        }

        if (addonManager.enableAddon(addonId)) {
            sendPrefixed(sender, MessageKey.of("command.addon.enable-success"), "addon", addonId);
        } else {
            sendPrefixed(sender, MessageKey.of("command.addon.enable-failed"), "addon", addonId);
        }
    }

    @Command("croppy addon disable <addonId>")
    @Permission("croppy.admin.addon")
    public void disableAddon(
            Source source,
            @Argument(value = "addonId", suggestions = "addon_ids") String addonId
    ) {
        CommandSender sender = source.source();
        if (addonManager == null) return;

        if ("all".equalsIgnoreCase(addonId)) {
            addonManager.disableAll();
            sendPrefixed(sender, MessageKey.of("command.addon.disable-all-success"));
            return;
        }

        if (addonManager.disableAddon(addonId)) {
            sendPrefixed(sender, MessageKey.of("command.addon.disable-success"), "addon", addonId);
        } else {
            sendPrefixed(sender, MessageKey.of("command.addon.disable-failed"), "addon", addonId);
        }
    }

    @Command("croppy addon unload <addonId>")
    @Permission("croppy.admin.addon")
    public void unloadAddon(
            Source source,
            @Argument(value = "addonId", suggestions = "addon_ids") String addonId
    ) {
        CommandSender sender = source.source();
        if (addonManager == null) return;

        if ("all".equalsIgnoreCase(addonId)) {
            int count = 0;
            for (dev.darkblade.croppy.api.addon.AddonDescription desc : addonManager.getDiscoveredAddons()) {
                if (addonManager.unloadAddon(desc.id())) count++;
            }
            sendPrefixed(sender, MessageKey.of("command.addon.unload-all-success"), "count", count);
            return;
        }

        if (addonManager.unloadAddon(addonId)) {
            sendPrefixed(sender, MessageKey.of("command.addon.unload-success"), "addon", addonId);
        } else {
            sendPrefixed(sender, MessageKey.of("command.addon.unload-failed"), "addon", addonId);
        }
    }

    @Suggestions("online_players")
    public List<String> suggestOnlinePlayers(CommandContext<Source> context, CommandInput input) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Command("croppy pack build")
    @Permission("croppy.admin.pack")
    public void buildPack(Source source) {
        CommandSender sender = source.source();
        if (resourcePackService == null) {
            sendPrefixed(sender, MessageKey.of("command.pack.service-unavailable"));
            return;
        }

        sendPrefixed(sender, MessageKey.of("command.pack.building"));

        resourcePackService.buildPackAsync().whenComplete((result, throwable) -> {
            if (throwable != null) {
                sendPrefixed(sender, MessageKey.of("command.pack.build-failed"),
                        "error", throwable.getMessage() != null ? throwable.getMessage() : "Unknown error");
            } else {
                double sizeMb = result.sizeBytes() / (1024.0 * 1024.0);
                sendRaw(sender, MessageKey.of("command.pack.build-success"),
                        "duration", String.valueOf(result.durationMs()),
                        "size", String.format(Locale.US, "%.2f", sizeMb),
                        "count", String.valueOf(result.assetCount()),
                        "hash", result.sha1Hex());
            }
        });
    }

    @Command("croppy pack send [targetPlayer]")
    @Permission("croppy.admin.pack")
    public void sendPack(
            Source source,
            @Argument(value = "targetPlayer", suggestions = "online_players") @Nullable String targetPlayer
    ) {
        CommandSender sender = source.source();
        if (resourcePackService == null) {
            sendPrefixed(sender, MessageKey.of("command.pack.service-unavailable"));
            return;
        }

        if (resourcePackService.getActivePack().isEmpty()) {
            sendPrefixed(sender, MessageKey.of("command.pack.no-pack"));
            return;
        }

        if (resourcePackService.resolvePublicUrl() == null) {
            sendPrefixed(sender, MessageKey.of("command.pack.no-url"));
            return;
        }

        if (targetPlayer == null || targetPlayer.isBlank()) {
            if (sender instanceof Player player) {
                boolean sent = resourcePackService.sendPack(player.getUniqueId());
                if (sent) {
                    sendPrefixed(sender, MessageKey.of("command.pack.sent"), "player", player.getName());
                }
            } else {
                int count = resourcePackService.sendPackToAll();
                sendPrefixed(sender, MessageKey.of("command.pack.sent-all"), "count", count);
            }
            return;
        }

        Player target = Bukkit.getPlayerExact(targetPlayer);
        if (target == null || !target.isOnline()) {
            sendPrefixed(sender, MessageKey.of("general.player-not-found"), "player", targetPlayer);
            return;
        }

        boolean sent = resourcePackService.sendPack(target.getUniqueId());
        if (sent) {
            sendPrefixed(sender, MessageKey.of("command.pack.sent"), "player", target.getName());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Plugin Update Administrative Commands
    // ---------------------------------------------------------------------------------------------

    @Command("croppy update check")
    @Permission("croppy.admin.update")
    public void updateCheck(Source source) {
        CommandSender sender = source.source();
        if (updateService == null || !updateService.isEnabled()) {
            sendPrefixed(sender, MessageKey.of("command.update.disabled"));
            return;
        }

        sendPrefixed(sender, MessageKey.of("command.update.checking"), "channel", updateService.getChannel().name());

        updateService.checkForUpdatesAsync(true).thenAccept(result -> {
            if (result.isFailed()) {
                sendPrefixed(sender, MessageKey.of("command.update.failed"), "error", result.errorMessage().orElse("Unknown error"));
                return;
            }

            if (result.updateAvailable()) {
                sendPrefixed(sender, MessageKey.of("command.update.available"),
                        "latest", result.latestVersion().map(dev.darkblade.croppy.api.update.SemanticVersion::asString).orElse("?"),
                        "current", result.currentVersion().asString(),
                        "channel", updateService.getChannel().name()
                );
            } else {
                sendPrefixed(sender, MessageKey.of("command.update.up-to-date"),
                        "version", result.currentVersion().asString()
                );
            }
        }).exceptionally(ex -> {
            String errorMsg = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
            sendPrefixed(sender, MessageKey.of("command.update.failed"), "error", errorMsg);
            return null;
        });
    }

    @Command("croppy update apply")
    @Permission("croppy.admin.update")
    public void updateApply(Source source) {
        CommandSender sender = source.source();
        if (updateService == null || !updateService.isEnabled()) {
            sendPrefixed(sender, MessageKey.of("command.update.disabled"));
            return;
        }

        sendPrefixed(sender, MessageKey.of("command.update.verifying"));

        updateService.applyUpdateAsync().thenAccept(result -> {
            if (result.alreadyUpToDate()) {
                sendPrefixed(sender, MessageKey.of("command.update.already-latest"),
                        "version", plugin != null ? plugin.getPluginMeta().getVersion() : "current"
                );
                return;
            }

            if (result.success()) {
                sendPrefixed(sender, MessageKey.of("command.update.staged-success"),
                        "version", result.versionStaged().orElse("?"),
                        "file", result.stagedFileName().orElse("Croppy.jar")
                );
            } else {
                sendPrefixed(sender, MessageKey.of("command.update.failed"),
                        "error", result.errorMessage().orElse("Unknown error")
                );
            }
        }).exceptionally(ex -> {
            String errorMsg = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
            sendPrefixed(sender, MessageKey.of("command.update.failed"), "error", errorMsg);
            return null;
        });
    }

    @Suggestions("log_channels")
    public List<String> suggestLogChannels(CommandContext<Source> context, CommandInput input) {
        return Arrays.stream(LogChannel.values())
                .map(LogChannel::id)
                .toList();
    }

    @Command("croppy debug status")
    @Permission("croppy.admin")
    public void debugStatus(Source source) {
        CommandSender sender = source.source();
        PaperCroppyLogger logger = requirePaperLogger(sender);
        if (logger == null) {
            return;
        }
        LogLevel level = logger.getMinimumLevel();
        LogStyle style = pluginConfig != null ? pluginConfig.getLoggingStyle() : LogStyle.RICH;
        Set<LogChannel> active = logger.getDebugChannels();

        sendRaw(sender, MessageKey.of("command.debug.status-header"));
        sendRaw(sender, MessageKey.of("command.debug.status-config"),
                "level", level.name(),
                "style", style.name());
        sendRaw(sender, MessageKey.of("command.debug.status-channels-header"));
        for (LogChannel channel : LogChannel.values()) {
            boolean enabled = active.contains(channel);
            sendRaw(sender,
                    MessageKey.of(enabled ? "command.debug.channel-entry-on" : "command.debug.channel-entry-off"),
                    "channel", channel.id());
        }
    }

    @Command("croppy debug toggle <channel>")
    @Permission("croppy.admin")
    public void debugToggle(
            Source source,
            @Argument(value = "channel", suggestions = "log_channels") String channelInput
    ) {
        CommandSender sender = source.source();
        PaperCroppyLogger logger = requirePaperLogger(sender);
        LogChannel channel = logger != null ? parseDebugChannel(sender, channelInput) : null;
        if (logger == null || channel == null) {
            return;
        }
        boolean nowEnabled = logger.toggleDebugChannel(channel);
        emitChannelChange(sender, logger, channel, nowEnabled ? "enabled" : "disabled", true);
    }

    @Command("croppy debug enable <channel>")
    @Permission("croppy.admin")
    public void debugEnable(
            Source source,
            @Argument(value = "channel", suggestions = "log_channels") String channelInput
    ) {
        CommandSender sender = source.source();
        PaperCroppyLogger logger = requirePaperLogger(sender);
        LogChannel channel = logger != null ? parseDebugChannel(sender, channelInput) : null;
        if (logger == null || channel == null) {
            return;
        }
        boolean changed = logger.enableDebugChannel(channel);
        emitChannelChange(sender, logger, channel, changed ? "enabled" : "already-enabled", changed);
    }

    @Command("croppy debug disable <channel>")
    @Permission("croppy.admin")
    public void debugDisable(
            Source source,
            @Argument(value = "channel", suggestions = "log_channels") String channelInput
    ) {
        CommandSender sender = source.source();
        PaperCroppyLogger logger = requirePaperLogger(sender);
        LogChannel channel = logger != null ? parseDebugChannel(sender, channelInput) : null;
        if (logger == null || channel == null) {
            return;
        }
        boolean changed = logger.disableDebugChannel(channel);
        emitChannelChange(sender, logger, channel, changed ? "disabled" : "already-disabled", changed);
    }

    @Command("croppy debug all")
    @Permission("croppy.admin")
    public void debugAll(Source source) {
        CommandSender sender = source.source();
        PaperCroppyLogger logger = requirePaperLogger(sender);
        if (logger == null) {
            return;
        }
        logger.enableAllDebugChannels();
        logger.info(LogChannel.COMMAND, "Enabled console debug logging across all channels.");
        sendPrefixed(sender, MessageKey.of("command.debug.all-enabled"), "count", LogChannel.values().length);
    }

    @Command("croppy debug off")
    @Permission("croppy.admin")
    public void debugOff(Source source) {
        CommandSender sender = source.source();
        PaperCroppyLogger logger = requirePaperLogger(sender);
        if (logger == null) {
            return;
        }
        logger.disableAllDebugChannels();
        logger.info(LogChannel.COMMAND, "Cleared all runtime debug channel overrides.");
        sendPrefixed(sender, MessageKey.of("command.debug.all-disabled"));
    }

    private PaperCroppyLogger requirePaperLogger(CommandSender sender) {
        PaperCroppyLogger logger = resolvePaperLogger();
        if (logger == null) {
            sendPrefixed(sender, MessageKey.of("command.debug.unavailable"));
        }
        return logger;
    }

    private LogChannel parseDebugChannel(CommandSender sender, String channelInput) {
        Optional<LogChannel> channelOpt = LogChannel.fromString(channelInput);
        if (channelOpt.isPresent()) {
            return channelOpt.get();
        }
        String validChannels = Arrays.stream(LogChannel.values())
                .map(LogChannel::id)
                .collect(Collectors.joining(", "));
        sendPrefixed(sender, MessageKey.of("command.debug.invalid-channel"),
                "channel", channelInput != null ? channelInput : "",
                "valid", validChannels);
        return null;
    }

    private void emitChannelChange(
            CommandSender sender,
            PaperCroppyLogger logger,
            LogChannel channel,
            String messageSuffix,
            boolean changed
    ) {
        if (changed) {
            logger.info(LogChannel.COMMAND,
                    "Console debug channel '" + channel.id() + "' -> " + messageSuffix + ".");
        }
        sendPrefixed(sender, MessageKey.of("command.debug." + messageSuffix), "channel", channel.id());
    }
}
