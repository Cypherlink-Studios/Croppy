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

        commands.setRecipeServices(recipeRegistry, discoveryEngine, null, null);

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
    }

    @Test
    @DisplayName("Verify /croppy addon subcommands route and execute cleanly")
    void testAddonCommandsExecution() {
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

        dev.darkblade.croppy.platform.paper.addon.PaperAddonManager manager =
                new dev.darkblade.croppy.platform.paper.addon.PaperAddonManager(null, null, java.nio.file.Path.of("addons"), null);
        dev.darkblade.croppy.api.addon.AddonDescription desc = new dev.darkblade.croppy.api.addon.AddonDescription(
                "orchard", "Orchard Addon", "1.0.0", "orchard", java.util.Optional.empty(), List.of("DarkBladeDev"), "Fruit trees", List.of(), List.of()
        );
        manager.registerDiscovered(desc, java.nio.file.Path.of("addons/orchard"));
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
