package dev.darkblade.croppy.platform.paper.station;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.item.DefaultItemRegistry;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.DefaultStationManager;
import dev.darkblade.croppy.core.station.DefaultStationRegistry;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.platform.paper.item.ItemFactory;
import dev.darkblade.croppy.platform.paper.station.listener.StationBlockListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StationBlockHeatSourceAttachmentTest {

    @Test
    @DisplayName("Heat source properties configure DefaultHeatSourceComponent bounds correctly")
    void testHeatSourceComponentCreationFromProperties() {
        Key stoveKey = Key.of("croppy", "stove");
        StationDefinition stoveDef = new StationDefinition(
                stoveKey,
                "Rustic Stove",
                "minecraft:smoker",
                StationInteractionType.HYBRID,
                1,
                0,
                true,
                List.of(),
                Map.of(
                        "heat_source", "true",
                        "min_temperature", "30.0",
                        "max_temperature", "280.0",
                        "heating_rate", "4.0",
                        "cooling_rate", "1.5",
                        "fuel_burn_multiplier", "1.2"
                )
        );

        DefaultStationRegistry registry = new DefaultStationRegistry();
        registry.register(stoveDef);

        DefaultStationManager manager = new DefaultStationManager();
        BlockPosition pos = BlockPosition.of("world", 10, 64, 10);
        DefaultStationInstance station = new DefaultStationInstance(stoveKey, pos, stoveDef);

        // Verify property parsing matching StationBlockListener logic
        boolean isHeatSource = Boolean.parseBoolean(stoveDef.properties().getOrDefault("heat_source", "false"));
        assertTrue(isHeatSource);

        double minTemp = Double.parseDouble(stoveDef.properties().get("min_temperature"));
        double maxTemp = Double.parseDouble(stoveDef.properties().get("max_temperature"));
        double heatingRate = Double.parseDouble(stoveDef.properties().get("heating_rate"));
        double coolingRate = Double.parseDouble(stoveDef.properties().get("cooling_rate"));
        double fuelMult = Double.parseDouble(stoveDef.properties().get("fuel_burn_multiplier"));

        DefaultHeatSourceComponent heatComp = new DefaultHeatSourceComponent(
                false, minTemp, DefaultHeatSourceComponent.DEFAULT_AMBIENT_TEMPERATURE, 0L, 0L,
                minTemp, maxTemp, heatingRate, coolingRate, fuelMult
        );
        station.setComponent(HeatSourceComponent.class, heatComp);

        manager.addStation(station);

        Optional<HeatSourceComponent> componentOpt = manager.getStation(pos).flatMap(s -> s.getComponent(HeatSourceComponent.class));
        assertTrue(componentOpt.isPresent());

        HeatSourceComponent component = componentOpt.get();
        assertEquals(30.0, component.getMinTemperature());
        assertEquals(280.0, component.getMaxTemperature());
        assertEquals(4.0, component.getHeatingRate());
        assertEquals(1.5, component.getCoolingRate());
        assertEquals(1.2, component.getFuelBurnMultiplier());
        assertFalse(component.isActive());
        assertEquals(20.0, component.getOutputTemperature());
    }
}
