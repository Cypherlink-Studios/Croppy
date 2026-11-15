package dev.darkblade.croppy.core.station.visual;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.process.ProcessState;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.station.component.HeatSourceComponent;
import dev.darkblade.croppy.api.station.component.ThermalComponent;
import dev.darkblade.croppy.api.variable.TypedVariableStore;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.process.DefaultProcessInstance;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import dev.darkblade.croppy.core.station.StationStateEvaluator;
import dev.darkblade.croppy.core.station.component.DefaultHeatSourceComponent;
import dev.darkblade.croppy.core.station.component.DefaultThermalComponent;
import dev.darkblade.croppy.core.variable.IntegerDataType;
import dev.darkblade.croppy.core.variable.MapCompoundDataType;
import dev.darkblade.croppy.core.variable.StringDataType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextualTextFormatterTest {

    private static StationDefinition dummyDefinition(String id) {
        return new StationDefinition(
                Key.of("croppy", id),
                id,
                "minecraft:barrier",
                StationInteractionType.HYBRID,
                4,
                1,
                StationDefinition.DEFAULT_MENU_KEY,
                true,
                List.of(),
                Map.of(),
                null
        );
    }

    @Test
    void testFormatCookingPotContext() {
        StationDefinition def = dummyDefinition("cooking_pot");
        DefaultStationInstance pot = new DefaultStationInstance(
                def.key(), new BlockPosition("world", 0, 64, 0), def);

        DefaultThermalComponent thermal = new DefaultThermalComponent();
        thermal.setTemperature(95.25);
        pot.setComponent(ThermalComponent.class, thermal);

        DefaultProcessInstance process = new DefaultProcessInstance(
                Key.of("croppy", "soup"), ProcessState.RUNNING, 0, 100L, 50L);
        process.setContextVariable("progress", 0.5);

        EvaluationContext ctx = StationStateEvaluator.buildContext(pot, process)
                .with("recipe_name", "Tomato Soup");

        String result = ContextualTextFormatter.format(
                "<yellow>{recipe_name}</yellow> [{process.progress_percent}%] {process.progress_bar} Temp: {thermal.temp_c}",
                ctx
        );
        assertEquals(
                "<yellow>Tomato Soup</yellow> [50%] <green>■■■■■</green><gray>□□□□□</gray> Temp: 95.3°C",
                result
        );
    }

    @Test
    void testFormatStoveWithoutProcess() {
        StationDefinition def = dummyDefinition("stove");
        DefaultStationInstance stove = new DefaultStationInstance(
                def.key(), new BlockPosition("world", 1, 64, 1), def);

        DefaultHeatSourceComponent heat = new DefaultHeatSourceComponent();
        heat.startBurn(1200);
        heat.setActive(true);
        stove.setComponent(HeatSourceComponent.class, heat);

        DefaultThermalComponent thermal = new DefaultThermalComponent();
        thermal.setTemperature(180.0);
        stove.setComponent(ThermalComponent.class, thermal);

        EvaluationContext ctx = StationStateEvaluator.buildContext(stove, null);
        String result = ContextualTextFormatter.format(
                "Fuel: {heat.fuel_ticks} | Active: {heat.is_active} | Temp: {thermal.temp_c}",
                ctx
        );
        assertEquals("Fuel: 1200 | Active: true | Temp: 180.0°C", result);
    }

    @Test
    void testFormatCustomSimpleAndCompoundVariables() {
        StationDefinition def = dummyDefinition("fermenter");
        DefaultStationInstance station = new DefaultStationInstance(
                def.key(), new BlockPosition("world", 2, 64, 2), def);

        TypedVariableStore store = station.ensureVariableStore();
        store.define("cleanliness", IntegerDataType.INSTANCE, 85L);
        store.define("label", StringDataType.INSTANCE, "Oak Barrel");
        store.define("tank", MapCompoundDataType.INSTANCE, Map.of("fluid", "brine", "level", 40L));

        EvaluationContext ctx = StationStateEvaluator.buildContext(station, null);
        String result = ContextualTextFormatter.format(
                "{var.label}: {var.cleanliness}% | Tank: {var.tank.fluid} ({var.tank.level}mb) | {unknown.token}",
                ctx
        );
        assertEquals("Oak Barrel: 85% | Tank: brine (40mb) | {unknown.token}", result);
    }

    @Test
    void testEdgeCasesNullAndMalformedBraces() {
        EvaluationContext ctx = EvaluationContext.empty().with("a.b", "ok");
        assertEquals("", ContextualTextFormatter.format(null, ctx));
        assertEquals("", ContextualTextFormatter.format("", ctx));
        assertEquals("unclosed {a.b", ContextualTextFormatter.format("unclosed {a.b", ctx));
        assertEquals("nested {prefix ok}", ContextualTextFormatter.format("nested {prefix {a.b}}", ctx));
    }
}
