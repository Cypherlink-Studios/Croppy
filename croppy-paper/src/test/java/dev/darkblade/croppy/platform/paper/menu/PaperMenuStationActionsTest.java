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
}
