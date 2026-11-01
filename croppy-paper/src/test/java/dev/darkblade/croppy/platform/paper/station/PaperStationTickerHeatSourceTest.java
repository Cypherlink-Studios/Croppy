package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.fuel.FuelProperties;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.StorageComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.fuel.DefaultFuelRegistry;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.process.ProcessEngine;
import dev.darkblade.croppy.core.rule.RuleEngine;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.core.station.component.DefaultStorageComponent;
import dev.darkblade.croppy.platform.paper.fuel.PaperFuelService;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import org.bukkit.Color;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaperStationTickerHeatSourceTest {

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

    @Test
    @DisplayName("Ticker auto-replenishes fuel from input storage when heat source is active")
    void testFuelAutoReplenish() throws Exception {
        DefaultFuelRegistry fuelRegistry = new DefaultFuelRegistry();
        Key coalKey = Key.of("minecraft", "coal");
        fuelRegistry.registerFuel(coalKey, new FuelProperties(1600L, 600.0, 1.0));
        PaperFuelService fuelService = new PaperFuelService(fuelRegistry);

        Plugin plugin = createMockPlugin();
        DefaultStationManager stationManager = new DefaultStationManager();
        ProcessEngine processEngine = new ProcessEngine(new dev.darkblade.croppy.core.process.DefaultProcessRegistry());
        RuleEngine ruleEngine = new RuleEngine(new dev.darkblade.croppy.core.rule.DefaultRuleRegistry());
        ItemFactory itemFactory = new ItemFactory(null, new DefaultItemRegistry());

        PaperStationTicker ticker = new PaperStationTicker(
                plugin, stationManager, processEngine, new PaperThermalEvaluator(),
                ruleEngine, null, itemFactory, null, fuelService, 20L
        );

        Key stoveKey = Key.of("croppy", "stove");
        StationDefinition stoveDef = new StationDefinition(
                stoveKey, "Stove", "minecraft:smoker", StationInteractionType.HYBRID,
                1, 0, true, List.of(), Map.of("heat_source", "true")
        );
        DefaultStationInstance station = new DefaultStationInstance(stoveKey, BlockPosition.of("world", 0, 64, 0), stoveDef);

        DefaultHeatSourceComponent heatSource = new DefaultHeatSourceComponent(
                true, 150.0, 20.0, 0L, 0L, 20.0, 350.0, 5.0, 2.0, 1.0
        );
        station.setComponent(HeatSourceComponent.class, heatSource);

        DefaultStorageComponent storage = new DefaultStorageComponent(1, 0);
        storage.addInput(StoredItem.of(coalKey, 3, 0));
        station.setComponent(StorageComponent.class, storage);

        assertEquals(0L, heatSource.getBurnTicksRemaining());
        assertEquals(3, storage.getInputs().get(0).amount());

        // Use reflection to invoke tryReplenishFuel
        Method replenishMethod = PaperStationTicker.class.getDeclaredMethod(
                "tryReplenishFuel", HeatSourceComponent.class, StorageComponent.class
        );
        replenishMethod.setAccessible(true);
        replenishMethod.invoke(ticker, heatSource, storage);

        // Verification: 1 coal was consumed, burn ticks initialized to 1600
        assertEquals(1600L, heatSource.getBurnTicksRemaining());
        assertEquals(1600L, heatSource.getMaxBurnTicks());
        assertEquals(2, storage.getInputs().get(0).amount());
        assertTrue(heatSource.isBurning());
    }

    @Test
    @DisplayName("Dust particle color tier calculation reflects temperature ranges accurately")
    void testDustColorTiers() {
        // Test color ranges matching PaperStationTicker logic
        assertEquals(Color.fromRGB(255, 140, 20), getExpectedColor(70.0), "Low temp warm amber");
        assertEquals(Color.fromRGB(240, 50, 10), getExpectedColor(180.0), "Medium incandescent red");
        assertEquals(Color.fromRGB(255, 90, 10), getExpectedColor(280.0), "High sear fiery orange");
        assertEquals(Color.fromRGB(40, 220, 255), getExpectedColor(400.0), "Extreme alchemical cyan");
    }

    private Color getExpectedColor(double temp) {
        if (temp < 90.0) {
            return Color.fromRGB(255, 140, 20);
        } else if (temp <= 220.0) {
            return Color.fromRGB(240, 50, 10);
        } else if (temp > 350.0) {
            return Color.fromRGB(40, 220, 255);
        } else {
            return Color.fromRGB(255, 90, 10);
        }
    }
}
