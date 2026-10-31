package dev.darkblade.croppy.core.interaction;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionAction;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionTrigger;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInteractionType;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.core.station.DefaultStationInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionRouterTest {

    private DefaultInteractionRegistry registry;
    private InteractionRouter router;
    private DefaultStationInstance potStation;

    @BeforeEach
    void setUp() {
        registry = new DefaultInteractionRegistry();
        router = new InteractionRouter(registry);

        Key potKey = Key.of("croppy", "cooking_pot");
        StationDefinition def = new StationDefinition(
                potKey,
                "Cooking Pot",
                "minecraft:player_head",
                StationInteractionType.HYBRID,
                6,
                2,
                true,
                List.of(),
                Map.of()
        );
        potStation = new DefaultStationInstance(potKey, new BlockPosition("world", 0, 64, 0), def);
    }

    @Test
    @DisplayName("Routes sneak right click to GUI by priority")
    void testSneakRightClickRouting() {
        InteractionDefinition guiDef = new InteractionDefinition(
                Key.of("croppy", "pot_gui"),
                Optional.of(potStation.getStationKey()),
                InteractionTrigger.SNEAK_RIGHT_CLICK,
                Optional.empty(),
                Optional.empty(),
                false,
                List.of(),
                List.of(InteractionAction.of("OPEN_GUI")),
                100
        );
        registry.register(guiDef);

        Optional<InteractionDefinition> match = router.match(
                potStation,
                InteractionTrigger.SNEAK_RIGHT_CLICK,
                null,
                null,
                true,
                EvaluationContext.empty()
        );

        assertTrue(match.isPresent());
        assertEquals("croppy:pot_gui", match.get().key().asString());
    }

    @Test
    @DisplayName("Matches specific custom tool and fails if item does not match")
    void testMatchItemFiltering() {
        InteractionDefinition thermometerDef = new InteractionDefinition(
                Key.of("croppy", "pot_thermometer"),
                Optional.of(potStation.getStationKey()),
                InteractionTrigger.RIGHT_CLICK,
                Optional.of(Key.of("croppy", "thermometer")),
                Optional.empty(),
                false,
                List.of(),
                List.of(InteractionAction.of("SHOW_TEMPERATURE")),
                50
        );
        registry.register(thermometerDef);

        // Right click with thermometer -> match
        Optional<InteractionDefinition> match = router.match(
                potStation,
                InteractionTrigger.RIGHT_CLICK,
                Key.of("croppy", "thermometer"),
                "minecraft:lightning_rod",
                false,
                EvaluationContext.empty()
        );
        assertTrue(match.isPresent());
        assertEquals("croppy:pot_thermometer", match.get().key().asString());

        // Right click with other item -> no match
        Optional<InteractionDefinition> noMatch = router.match(
                potStation,
                InteractionTrigger.RIGHT_CLICK,
                Key.of("croppy", "spoon"),
                "minecraft:stick",
                false,
                EvaluationContext.empty()
        );
        assertTrue(noMatch.isEmpty());
    }

    @Test
    @DisplayName("Evaluates condition expression properly")
    void testConditionEvaluation() {
        InteractionDefinition conditionalDef = new InteractionDefinition(
                Key.of("croppy", "pot_serve"),
                Optional.of(potStation.getStationKey()),
                InteractionTrigger.RIGHT_CLICK,
                Optional.empty(),
                Optional.of("minecraft:bowl"),
                false,
                List.of("process.finished == true"),
                List.of(InteractionAction.of("SERVE_OUTPUT")),
                30
        );
        registry.register(conditionalDef);

        // When process.finished is false -> no match
        EvaluationContext falseCtx = EvaluationContext.empty().with("process.finished", false);
        Optional<InteractionDefinition> noMatch = router.match(
                potStation,
                InteractionTrigger.RIGHT_CLICK,
                null,
                "minecraft:bowl",
                false,
                falseCtx
        );
        assertTrue(noMatch.isEmpty());

        // When process.finished is true -> matches
        EvaluationContext trueCtx = EvaluationContext.empty().with("process.finished", true);
        Optional<InteractionDefinition> match = router.match(
                potStation,
                InteractionTrigger.RIGHT_CLICK,
                null,
                "minecraft:bowl",
                false,
                trueCtx
        );
        assertTrue(match.isPresent());
        assertEquals("croppy:pot_serve", match.get().key().asString());
    }
}
