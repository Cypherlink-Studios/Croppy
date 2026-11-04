package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.menu.ClickTypeFilter;
import dev.darkblade.croppy.api.menu.MenuAction;
import dev.darkblade.croppy.api.menu.MenuActionType;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.SlotDefinition;
import dev.darkblade.croppy.api.menu.SlotType;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.prompt.ChatPromptService;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.recipe.RecipeDefinition;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.ProcessComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.core.process.DefaultProcessRegistry;
import dev.darkblade.croppy.core.recipe.DefaultRecipeRegistry;
import dev.darkblade.croppy.core.station.component.DefaultProcessComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PaperMenuStationActionsTest {

    private Plugin createMockPlugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("isEnabled".equals(method.getName())) return false;
                    if ("getName".equals(method.getName())) return "Croppy";
                    return null;
                }
        );
    }

    private Player createMockPlayer(UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return uuid;
                    if ("getLocation".equals(method.getName())) return new Location(null, 0, 0, 0);
                    return null;
                }
        );
    }

    @Test
    @DisplayName("TOGGLE_POWER, ADJUST_TEMP, and PROMPT_CHAT_TEMP station actions update heat source component")
    void testStationHeatActions() throws Exception {
        Plugin plugin = createMockPlugin();
        ItemFactory itemFactory = new ItemFactory(null, new DefaultItemRegistry());
        PaperMenuRenderer renderer = new PaperMenuRenderer(itemFactory);
        ChatPromptService promptService = new ChatPromptService(plugin);

        PaperMenuListener listener = new PaperMenuListener(
                plugin,
                new ProcessEngine(new dev.darkblade.croppy.core.process.DefaultProcessRegistry()),
                itemFactory,
                renderer,
                promptService,
                null
        );

        Key stoveKey = Key.of("croppy", "stove");
        StationDefinition stoveDef = new StationDefinition(
                stoveKey, "Stove", "minecraft:smoker", StationInteractionType.HYBRID,
                1, 0, true, List.of(), Map.of("heat_source", "true")
        );
        DefaultStationInstance station = new DefaultStationInstance(stoveKey, BlockPosition.of("world", 0, 64, 0), stoveDef);
        DefaultHeatSourceComponent heatSource = new DefaultHeatSourceComponent(
                false, 100.0, 20.0, 0L, 0L, 20.0, 300.0, 5.0, 2.0, 1.0
        );
        station.setComponent(HeatSourceComponent.class, heatSource);

        MenuDefinition menuDef = new MenuDefinition(
                Key.of("croppy", "stove_menu"),
                "Stove Control",
                3,
                List.of(
                        "#########",
                        "#########",
                        "#########"
                ),
                Map.of(),
                Map.of()
        );
        PaperMenuHolder holder = new PaperMenuHolder(menuDef, station, dev.darkblade.croppy.api.expression.EvaluationContext.empty());
        Player player = createMockPlayer(UUID.randomUUID());

        // Use reflection to call executeAction
        Method executeActionMethod = PaperMenuListener.class.getDeclaredMethod(
                "executeAction", Player.class, PaperMenuHolder.class, MenuAction.class
        );
        executeActionMethod.setAccessible(true);

        // 1. Test TOGGLE_POWER
        assertFalse(heatSource.isActive());
        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.ALL, MenuActionType.STATION_ACTION, "TOGGLE_POWER", null));
        assertTrue(heatSource.isActive());

        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.ALL, MenuActionType.STATION_ACTION, "TOGGLE_POWER", null));
        assertFalse(heatSource.isActive());

        // 2. Test ADJUST_TEMP:+10
        assertEquals(100.0, heatSource.getTargetTemperature());
        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.ALL, MenuActionType.STATION_ACTION, "ADJUST_TEMP:+10", null));
        assertEquals(110.0, heatSource.getTargetTemperature());

        // 3. Test ADJUST_TEMP:-10
        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.ALL, MenuActionType.STATION_ACTION, "ADJUST_TEMP:-10", null));
        assertEquals(100.0, heatSource.getTargetTemperature());

        // 4. Test INCREASE_HEAT / DECREASE_HEAT keywords
        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.ALL, MenuActionType.STATION_ACTION, "INCREASE_HEAT", null));
        assertEquals(110.0, heatSource.getTargetTemperature());
        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.ALL, MenuActionType.STATION_ACTION, "DECREASE_HEAT", null));
        assertEquals(100.0, heatSource.getTargetTemperature());

        // 5. Test PROMPT_CHAT_TEMP
        assertFalse(promptService.hasActivePrompt(player.getUniqueId()));
        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.ALL, MenuActionType.STATION_ACTION, "PROMPT_CHAT_TEMP", null));
        assertTrue(promptService.hasActivePrompt(player.getUniqueId()));
    }

    @Test
    @DisplayName("START_PROCESS action starts recipe process, consumes inputs, and updates holder context")
    void testStartProcessActionInCookingPotMenu() throws Exception {
        Plugin plugin = createMockPlugin();
        ItemFactory itemFactory = new ItemFactory(null, new DefaultItemRegistry());
        PaperMenuRenderer renderer = new PaperMenuRenderer(itemFactory);

        DefaultProcessRegistry processRegistry = new DefaultProcessRegistry();
        DefaultRecipeRegistry recipeRegistry = new DefaultRecipeRegistry();
        ProcessEngine processEngine = new ProcessEngine(processRegistry, recipeRegistry);

        PaperMenuListener listener = new PaperMenuListener(
                plugin,
                processEngine,
                itemFactory,
                renderer,
                null,
                null
        );

        Key potKey = Key.of("croppy", "cooking_pot");
        Key procKey = Key.of("croppy", "tomato_soup_cook");
        Key tomatoKey = Key.of("croppy", "tomato");
        Key bowlKey = Key.of("minecraft", "bowl");
        Key soupKey = Key.of("croppy", "tomato_soup");

        // 1. Register process (requires temp >= 75)
        dev.darkblade.croppy.api.expression.Expression tempCond =
                dev.darkblade.croppy.core.expression.ExpressionCompiler.compile("station.temperature >= 75");
        ProcessDefinition procDef = new ProcessDefinition(
                procKey, "Tomato Soup Cooking", potKey, List.of(), tempCond,
                List.of(PhaseDefinition.timed("simmer", 300, tempCond)),
                null, null, 1, Map.of()
        );
        processRegistry.register(procDef);

        // 2. Register recipe
        RecipeDefinition recipe = new RecipeDefinition(
                soupKey, "Tomato Soup", Key.of("croppy", "soups"), potKey, procKey,
                List.of(StoredItem.of(tomatoKey, 2), StoredItem.of(bowlKey, 1)),
                soupKey, 1, null, false, List.of(), Map.of()
        );
        recipeRegistry.register(recipe);

        // 3. Station setup
        StationDefinition potDef = new StationDefinition(
                potKey, "Cooking Pot", "minecraft:player_head", StationInteractionType.HYBRID,
                6, 2, true, List.of(), Map.of("thermal", "true")
        );
        DefaultStationInstance station = new DefaultStationInstance(potKey, BlockPosition.of("world", 10, 64, 10), potDef);
        DefaultStorageComponent storage = new DefaultStorageComponent(6, 2);
        storage.addInput(StoredItem.of(tomatoKey, 2));
        storage.addInput(StoredItem.of(bowlKey, 1));
        station.setComponent(StorageComponent.class, storage);
        station.setComponent(ProcessComponent.class, new DefaultProcessComponent());
        station.setComponent(ThermalComponent.class, new DefaultThermalComponent(95.0, 95.0, true)); // Hot: 95°C

        MenuDefinition menuDef = new MenuDefinition(
                Key.of("croppy", "cooking_pot"),
                "Cooking Pot",
                4,
                List.of("#########", "#########", "#########", "#########"),
                Map.of(),
                Map.of()
        );

        dev.darkblade.croppy.api.expression.EvaluationContext baseContext = dev.darkblade.croppy.api.expression.EvaluationContext.empty()
                .with("station.temperature", 95.0)
                .with("station.heat_level", 95L)
                .with("station.is_heated", true)
                .with("process.state", "IDLE");

        PaperMenuHolder holder = new PaperMenuHolder(menuDef, station, baseContext);
        Player player = createMockPlayer(UUID.randomUUID());

        Method executeActionMethod = PaperMenuListener.class.getDeclaredMethod(
                "executeAction", Player.class, PaperMenuHolder.class, MenuAction.class
        );
        executeActionMethod.setAccessible(true);

        // 4. Click START_PROCESS
        executeActionMethod.invoke(listener, player, holder, new MenuAction(ClickTypeFilter.LEFT, MenuActionType.STATION_ACTION, "START_PROCESS", null));

        // 5. Verify process started and inputs consumed
        assertTrue(storage.getInputs().isEmpty(), "Recipe inputs must be consumed upon start");
        ProcessComponent procComp = station.getComponent(ProcessComponent.class).orElseThrow();
        assertTrue(procComp.hasActiveProcess(), "Station must have an active process");
        assertEquals(ProcessState.RUNNING, procComp.getActiveProcess().get().getState());
        assertEquals(procKey, procComp.getActiveProcess().get().getProcessKey());
        assertEquals("RUNNING", holder.getContext().getVariable("process.state").orElse(null));
    }
}
