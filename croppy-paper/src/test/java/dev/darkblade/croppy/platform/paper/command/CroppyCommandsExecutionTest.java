package dev.darkblade.croppy.platform.paper.command;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.core.crop.CropManager;
import dev.darkblade.croppy.core.fertilizer.DefaultFertilizerRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.LogLevel;
import dev.darkblade.croppy.platform.paper.config.PluginConfig;
import dev.darkblade.croppy.platform.paper.logging.PaperCroppyLogger;
import dev.darkblade.croppy.platform.paper.service.CropInspectorService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.paper.util.sender.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CroppyCommandsExecutionTest {

    @Test
    @DisplayName("Verify /croppy item list executes without StackOverflowError and lists items correctly")
    void testItemListExecution() {
        DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
        DefaultFertilizerRegistry fertilizerRegistry = new DefaultFertilizerRegistry();
        CropInspectorService inspector = new CropInspectorService(
                new CropManager(),
                new DefaultSpeciesRegistry(),
                pos -> null
        );

        itemRegistry.register(new ItemDefinition(Key.of("croppy", "wheat_seeds"), "seeds", "WHEAT_SEEDS", 0, "Seeds", List.of(), Map.of()));
        itemRegistry.register(new ItemDefinition(Key.of("croppy", "copper_can"), "watering_cans", "COPPER_INGOT", 0, "Can", List.of(), Map.of()));

        CroppyCommands commands = new CroppyCommands(
                null,
                inspector,
                new PluginConfig(),
                null,
                null,
                fertilizerRegistry,
                null,
                itemRegistry
        );

        CommandManager<Source> commandManager = new CommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                CommandRegistrationHandler.nullCommandRegistrationHandler()
        ) {
            @Override
            public boolean hasPermission(Source sender, String permission) {
                return true;
            }
        };

        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(org.bukkit.entity.Player.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success((org.bukkit.entity.Player) null)
        );
        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(Key.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success(Key.of("croppy", "test"))
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>addonParser(null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.addon.AddonDescription>>() {})
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>recipeParser(null, null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.recipe.RecipeDefinition>>() {})
        );

        AnnotationParser<Source> parser = new AnnotationParser<>(commandManager, Source.class);
        parser.parse(commands);

        List<String> receivedMessages = new ArrayList<>();
        CommandSender mockSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args.length > 0) {
                        if (args[0] instanceof Component component) {
                            receivedMessages.add(PlainTextComponentSerializer.plainText().serialize(component));
                        } else if (args[0] != null) {
                            receivedMessages.add(args[0].toString());
                        }
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("source")) {
                        return mockSender;
                    }
                    return null;
                }
        );

        // 1. Execute without category: must not throw StackOverflowError and list all categories
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy item list").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Registered Items") || m.contains("Ítems Registrados")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("croppy:wheat_seeds")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("croppy:copper_can")));

        // 2. Execute with category: must list items under specified category
        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy item list seeds").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("seeds (1)")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("croppy:wheat_seeds")));

        // 3. Execute with non-existent category
        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy item list non_existent").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("non_existent")));
    }

    @Test
    @DisplayName("Verify /croppy recipe list executes and outputs registered recipes")
    void testRecipeCommandsExecution() {
        CropInspectorService inspector = new CropInspectorService(
                new CropManager(),
                new DefaultSpeciesRegistry(),
                pos -> null
        );

        CroppyCommands commands = new CroppyCommands(
                null,
                inspector,
                new PluginConfig(),
                null,
                null,
                new DefaultFertilizerRegistry(),
                null,
                new DefaultItemRegistry()
        );

        dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry recipeRegistry = new dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry();
        dev.darkblade.croppy.core.recipe.DefaultRecipeCategoryRegistry categoryRegistry = new dev.darkblade.croppy.core.recipe.DefaultRecipeCategoryRegistry();
        dev.darkblade.croppy.core.discovery.DefaultRecipeDiscoveryEngine discoveryEngine = new dev.darkblade.croppy.core.discovery.DefaultRecipeDiscoveryEngine();

        recipeRegistry.register(new dev.darkblade.croppy.api.recipe.RecipeDefinition(
                Key.of("croppy", "tomato_soup"),
                "Tomato Soup",
                dev.darkblade.croppy.api.recipe.RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "tomato_soup_p"),
                List.of(),
                Key.of("croppy", "tomato_soup"),
                1,
                dev.darkblade.croppy.api.recipe.HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        ));
        recipeRegistry.register(new dev.darkblade.croppy.api.recipe.RecipeDefinition(
                Key.of("croppy", "vegetable_stew"),
                "Vegetable Stew",
                dev.darkblade.croppy.api.recipe.RecipeCategory.SOUPS.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "vegetable_stew_p"),
                List.of(),
                Key.of("croppy", "vegetable_stew"),
                1,
                dev.darkblade.croppy.api.recipe.HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        ));
        recipeRegistry.register(new dev.darkblade.croppy.api.recipe.RecipeDefinition(
                Key.of("croppy", "berry_tea"),
                "Berry Tea",
                dev.darkblade.croppy.api.recipe.RecipeCategory.BEVERAGES.getKey(),
                Key.of("croppy", "cooking_pot"),
                Key.of("croppy", "berry_tea_p"),
                List.of(),
                Key.of("croppy", "berry_tea"),
                1,
                dev.darkblade.croppy.api.recipe.HintDefinition.DEFAULT,
                false,
                List.of(),
                Map.of()
        ));

        commands.setRecipeServices(recipeRegistry, discoveryEngine, null, null, categoryRegistry);

        CommandManager<Source> commandManager = new CommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                CommandRegistrationHandler.nullCommandRegistrationHandler()
        ) {
            @Override
            public boolean hasPermission(Source sender, String permission) {
                return true;
            }
        };

        List<String> targetMessages = new ArrayList<>();
        java.util.UUID steveId = java.util.UUID.randomUUID();
        org.bukkit.entity.Player mockSteve = (org.bukkit.entity.Player) Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class<?>[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return steveId;
                    if (method.getName().equals("getName")) return "Steve";
                    if (method.getName().equals("locale")) return java.util.Locale.ENGLISH;
                    if (method.getName().equals("sendMessage") && args != null && args.length > 0) {
                        if (args[0] instanceof Component c) {
                            targetMessages.add(PlainTextComponentSerializer.plainText().serialize(c));
                        }
                        return null;
                    }
                    return null;
                }
        );

        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(org.bukkit.entity.Player.class),
                params -> (context, input) -> {
                    input.readString();
                    return org.incendo.cloud.parser.ArgumentParseResult.success(mockSteve);
                }
        );
        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(Key.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success(Key.of("croppy", "test"))
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>addonParser(null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.addon.AddonDescription>>() {})
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>recipeParser(recipeRegistry, categoryRegistry)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.recipe.RecipeDefinition>>() {})
        );

        AnnotationParser<Source> parser = new AnnotationParser<>(commandManager, Source.class);
        parser.parse(commands);

        List<String> receivedMessages = new ArrayList<>();
        CommandSender mockSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args.length > 0) {
                        if (args[0] instanceof Component component) {
                            receivedMessages.add(PlainTextComponentSerializer.plainText().serialize(component));
                        } else if (args[0] != null) {
                            receivedMessages.add(args[0].toString());
                        }
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("source")) {
                        return mockSender;
                    }
                    return null;
                }
        );

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy recipe list").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Registered Recipes")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("croppy:tomato_soup")));

        receivedMessages.clear();
        targetMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy recipe give Steve croppy:tomato_soup").join());
        assertEquals(dev.darkblade.croppy.api.recipe.KnowledgeState.DISCOVERED,
                discoveryEngine.getKnowledgeState(steveId, Key.of("croppy", "tomato_soup")));
        assertEquals(1, targetMessages.size());

        receivedMessages.clear();
        targetMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy recipe give Steve @soups").join());
        assertEquals(dev.darkblade.croppy.api.recipe.KnowledgeState.DISCOVERED,
                discoveryEngine.getKnowledgeState(steveId, Key.of("croppy", "vegetable_stew")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Granted 1 recipes") && m.contains("@soups")));
        assertEquals(1, targetMessages.size());
        assertTrue(targetMessages.get(0).contains("1 new recipes"));

        receivedMessages.clear();
        targetMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy recipe give Steve @all").join());
        assertEquals(dev.darkblade.croppy.api.recipe.KnowledgeState.DISCOVERED,
                discoveryEngine.getKnowledgeState(steveId, Key.of("croppy", "berry_tea")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Granted 1 recipes") && m.contains("@all")));

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy recipe revoke Steve @soups").join());
        assertEquals(dev.darkblade.croppy.api.recipe.KnowledgeState.UNKNOWN,
                discoveryEngine.getKnowledgeState(steveId, Key.of("croppy", "tomato_soup")));
        assertEquals(dev.darkblade.croppy.api.recipe.KnowledgeState.UNKNOWN,
                discoveryEngine.getKnowledgeState(steveId, Key.of("croppy", "vegetable_stew")));
        assertEquals(dev.darkblade.croppy.api.recipe.KnowledgeState.DISCOVERED,
                discoveryEngine.getKnowledgeState(steveId, Key.of("croppy", "berry_tea")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Revoked 2 recipes")));

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy recipe revoke Steve @all").join());
        assertEquals(dev.darkblade.croppy.api.recipe.KnowledgeState.UNKNOWN,
                discoveryEngine.getKnowledgeState(steveId, Key.of("croppy", "berry_tea")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Revoked 1 recipes")));
    }

    @Test
    @DisplayName("Verify /croppy addon subcommands route and execute cleanly")
    void testAddonCommandsExecution(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws java.io.IOException {
        DefaultItemRegistry itemRegistry = new DefaultItemRegistry();
        DefaultFertilizerRegistry fertilizerRegistry = new DefaultFertilizerRegistry();
        CropInspectorService inspector = new CropInspectorService(
                new CropManager(),
                new DefaultSpeciesRegistry(),
                pos -> null
        );

        CroppyCommands commands = new CroppyCommands(
                null,
                inspector,
                new PluginConfig(),
                null,
                null,
                fertilizerRegistry,
                null,
                itemRegistry
        );

        java.nio.file.Path orchardDir = tempDir.resolve("orchard");
        java.nio.file.Files.createDirectories(orchardDir);
        java.nio.file.Files.writeString(orchardDir.resolve("addon.yml"), """
                id: "orchard"
                name: "Orchard Addon"
                version: "1.0.0"
                namespace: "orchard"
                authors:
                  - "DarkBladeDev"
                """, java.nio.charset.StandardCharsets.UTF_8);

        dev.darkblade.croppy.platform.paper.addon.PaperAddonManager manager =
                new dev.darkblade.croppy.platform.paper.addon.PaperAddonManager(null, null, tempDir, null);
        manager.discoverAddons();
        commands.setAddonManager(manager);

        CommandManager<Source> commandManager = new CommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                CommandRegistrationHandler.nullCommandRegistrationHandler()
        ) {
            @Override
            public boolean hasPermission(Source sender, String permission) {
                return true;
            }
        };

        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(org.bukkit.entity.Player.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success((org.bukkit.entity.Player) null)
        );
        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(Key.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success(Key.of("croppy", "test"))
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>addonParser(manager)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.addon.AddonDescription>>() {})
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>recipeParser(null, null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.recipe.RecipeDefinition>>() {})
        );

        AnnotationParser<Source> parser = new AnnotationParser<>(commandManager, Source.class);
        parser.parse(commands);

        List<String> receivedMessages = new ArrayList<>();
        CommandSender mockSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args.length > 0) {
                        if (args[0] instanceof Component component) {
                            receivedMessages.add(PlainTextComponentSerializer.plainText().serialize(component));
                        } else if (args[0] != null) {
                            receivedMessages.add(args[0].toString());
                        }
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("source")) {
                        return mockSender;
                    }
                    return null;
                }
        );

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon list").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Discovered Addons")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("orchard")));

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon status orchard").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Addon: Orchard Addon")));

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon enable @all").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Enabled all discovered addons")));

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon reload orchard").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("reloaded successfully")));

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon reload @all").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Reloaded")));

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon disable @all").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Disabled all active addons")));

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon unload @all").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Unloaded")));

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> commandManager.commandExecutor().executeCommand(mockSource, "croppy addon enable all").join()
        );
    }

    @Test
    @DisplayName("Verify /croppy update check and /croppy update apply dispatch cleanly with mock UpdateService")
    void testUpdateCommandsExecution() {
        CropInspectorService inspector = new CropInspectorService(
                new CropManager(),
                new DefaultSpeciesRegistry(),
                pos -> null
        );

        CroppyCommands commands = new CroppyCommands(
                null,
                inspector,
                new PluginConfig(),
                null,
                null,
                new DefaultFertilizerRegistry(),
                null,
                new DefaultItemRegistry()
        );

        dev.darkblade.croppy.api.update.UpdateService mockUpdateService = new dev.darkblade.croppy.api.update.UpdateService() {
            @Override
            public java.util.concurrent.CompletableFuture<dev.darkblade.croppy.api.update.UpdateCheckResult> checkForUpdatesAsync(boolean forceCheck) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                        dev.darkblade.croppy.api.update.UpdateCheckResult.available(
                                dev.darkblade.croppy.api.update.SemanticVersion.of(0, 5, 13),
                                dev.darkblade.croppy.api.update.SemanticVersion.of(0, 5, 14),
                                "v0.5.14",
                                "https://github.com/Cypherlink-Studios/Croppy",
                                "http://example.com/Croppy-0.5.14.jar",
                                "http://example.com/checksums.txt",
                                "2026-11-08T00:00:00Z"
                        )
                );
            }

            @Override
            public java.util.concurrent.CompletableFuture<dev.darkblade.croppy.api.update.UpdateApplyResult> applyUpdateAsync() {
                return java.util.concurrent.CompletableFuture.completedFuture(
                        dev.darkblade.croppy.api.update.UpdateApplyResult.success("Croppy.jar", "0.5.14", "Staged")
                );
            }

            @Override
            public java.util.Optional<dev.darkblade.croppy.api.update.UpdateCheckResult> getCachedResult() {
                return java.util.Optional.empty();
            }

            @Override
            public dev.darkblade.croppy.api.update.UpdateChannel getChannel() {
                return dev.darkblade.croppy.api.update.UpdateChannel.STABLE;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        commands.setUpdateService(mockUpdateService);

        CommandManager<Source> commandManager = new CommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                CommandRegistrationHandler.nullCommandRegistrationHandler()
        ) {
            @Override
            public boolean hasPermission(Source sender, String permission) {
                return true;
            }
        };

        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(org.bukkit.entity.Player.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success((org.bukkit.entity.Player) null)
        );
        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(Key.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success(Key.of("croppy", "test"))
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>addonParser(null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.addon.AddonDescription>>() {})
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>recipeParser(null, null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.recipe.RecipeDefinition>>() {})
        );

        AnnotationParser<Source> parser = new AnnotationParser<>(commandManager, Source.class);
        parser.parse(commands);

        List<String> receivedMessages = new ArrayList<>();
        CommandSender mockSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args.length > 0) {
                        if (args[0] instanceof Component component) {
                            receivedMessages.add(PlainTextComponentSerializer.plainText().serialize(component));
                        } else if (args[0] != null) {
                            receivedMessages.add(args[0].toString());
                        }
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("source")) {
                        return mockSender;
                    }
                    return null;
                }
        );

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy update check").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("0.5.14")));

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy update apply").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("plugins/update/Croppy.jar") || m.contains("Staged") || m.contains("staged")));
    }

    @Test
    @DisplayName("Verify /croppy debug subcommands execute via Cloud 2.0 and keep debug output console-only")
    void testDebugCommandsExecution() {
        CropInspectorService inspector = new CropInspectorService(
                new CropManager(),
                new DefaultSpeciesRegistry(),
                pos -> null
        );

        CroppyCommands commands = new CroppyCommands(
                null,
                inspector,
                new PluginConfig(),
                null,
                null,
                new DefaultFertilizerRegistry(),
                null,
                new DefaultItemRegistry()
        );

        List<String> consoleLogs = new ArrayList<>();
        PaperCroppyLogger paperLogger = new PaperCroppyLogger((level, message, throwable) ->
                consoleLogs.add(level.name() + " " + PlainTextComponentSerializer.plainText().serialize(message))
        );
        paperLogger.applyConfig(LogLevel.INFO, java.util.Set.of());
        commands.setCroppyLogger(paperLogger);

        CommandManager<Source> commandManager = new CommandManager<>(
                ExecutionCoordinator.simpleCoordinator(),
                CommandRegistrationHandler.nullCommandRegistrationHandler()
        ) {
            @Override
            public boolean hasPermission(Source sender, String permission) {
                return true;
            }
        };

        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(org.bukkit.entity.Player.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success((org.bukkit.entity.Player) null)
        );
        commandManager.parserRegistry().registerParserSupplier(
                io.leangen.geantyref.TypeToken.get(Key.class),
                params -> (context, input) -> org.incendo.cloud.parser.ArgumentParseResult.success(Key.of("croppy", "test"))
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>addonParser(null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.addon.AddonDescription>>() {})
        );
        commandManager.parserRegistry().registerParser(
                dev.darkblade.croppy.platform.paper.command.selector.CroppyTargetSelectors
                        .<Source>recipeParser(null, null)
                        .descriptor(new io.leangen.geantyref.TypeToken<dev.darkblade.croppy.platform.paper.command.selector.TargetSelection<dev.darkblade.croppy.api.recipe.RecipeDefinition>>() {})
        );

        AnnotationParser<Source> parser = new AnnotationParser<>(commandManager, Source.class);
        parser.parse(commands);

        List<String> receivedMessages = new ArrayList<>();
        CommandSender mockSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args.length > 0) {
                        if (args[0] instanceof Component component) {
                            receivedMessages.add(PlainTextComponentSerializer.plainText().serialize(component));
                        } else if (args[0] != null) {
                            receivedMessages.add(args[0].toString());
                        }
                    }
                    return null;
                }
        );

        Source mockSource = (Source) Proxy.newProxyInstance(
                Source.class.getClassLoader(),
                new Class<?>[]{Source.class},
                (proxy, method, args) -> method.getName().equals("source") ? mockSender : null
        );

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy debug enable packet").join());
        assertTrue(paperLogger.getDebugChannels().contains(LogChannel.PACKET));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("packet")));

        paperLogger.debug(LogChannel.PACKET, () -> "PACKET_CONSOLE_ONLY_PAYLOAD");
        assertTrue(consoleLogs.stream().anyMatch(m -> m.contains("PACKET_CONSOLE_ONLY_PAYLOAD")));
        assertTrue(receivedMessages.stream().noneMatch(m -> m.contains("PACKET_CONSOLE_ONLY_PAYLOAD")));

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy debug disable packet").join());
        assertTrue(paperLogger.getDebugChannels().isEmpty());

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy debug toggle station").join());
        assertTrue(paperLogger.getDebugChannels().contains(LogChannel.STATION));

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy debug status").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("INFO") && m.contains("RICH")));

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy debug all").join());
        assertTrue(paperLogger.getDebugChannels().containsAll(List.of(LogChannel.values())));

        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy debug off").join());
        assertTrue(paperLogger.getDebugChannels().isEmpty());

        receivedMessages.clear();
        assertDoesNotThrow(() -> commandManager.commandExecutor().executeCommand(mockSource, "croppy debug toggle nope").join());
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("nope") && m.contains("packet")));
    }
}
