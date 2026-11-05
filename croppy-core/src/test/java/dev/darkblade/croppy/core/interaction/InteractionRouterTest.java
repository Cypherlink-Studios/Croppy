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

    @Test
    @DisplayName("Routes sneak right click based on storage state conditions")
    void testStorageConditionRouting() {
        InteractionDefinition extractDef = new InteractionDefinition(
                Key.of("croppy", "skillet_extract"),
                Optional.of(potStation.getStationKey()),
                InteractionTrigger.SNEAK_RIGHT_CLICK,
                Optional.empty(),
                Optional.empty(),
                true,
                List.of("storage.has_inputs == true"),
                List.of(InteractionAction.of("EXTRACT_INPUTS")),
                80
        );

        InteractionDefinition guiDef = new InteractionDefinition(
                Key.of("croppy", "skillet_gui"),
                Optional.of(potStation.getStationKey()),
                InteractionTrigger.SNEAK_RIGHT_CLICK,
                Optional.empty(),
                Optional.empty(),
                true,
                List.of("storage.is_empty == true"),
                List.of(InteractionAction.of("OPEN_GUI")),
                70
        );

        registry.register(extractDef);
        registry.register(guiDef);

        // When storage has inputs -> matches extractDef
        EvaluationContext hasInputsCtx = EvaluationContext.empty()
                .with("storage.has_inputs", true)
                .with("storage.is_empty", false);

        Optional<InteractionDefinition> matchExtract = router.match(
                potStation,
                InteractionTrigger.SNEAK_RIGHT_CLICK,
                null,
                null,
                true,
                hasInputsCtx
        );
        assertTrue(matchExtract.isPresent());
        assertEquals("croppy:skillet_extract", matchExtract.get().key().asString());
        assertEquals("EXTRACT_INPUTS", matchExtract.get().actions().get(0).type());

        // When storage is empty -> matches guiDef
        EvaluationContext emptyCtx = EvaluationContext.empty()
                .with("storage.has_inputs", false)
                .with("storage.is_empty", true);

        Optional<InteractionDefinition> matchGui = router.match(
                potStation,
                InteractionTrigger.SNEAK_RIGHT_CLICK,
                null,
                null,
                true,
                emptyCtx
        );
        assertTrue(matchGui.isPresent());
        assertEquals("croppy:skillet_gui", matchGui.get().key().asString());
        assertEquals("OPEN_GUI", matchGui.get().actions().get(0).type());
    }

    @Test
    @DisplayName("Routes crop interactions with tools, conditions, and sneak triggers")
    void testCropInteractionRouting() {
        Key grapesKey = Key.of("croppy", "grapes");

        InteractionDefinition harvestHand = new InteractionDefinition(
                Key.of("croppy", "harvest_grapes_hand"),
                Optional.empty(),
                Optional.of(grapesKey),
                InteractionTrigger.RIGHT_CLICK,
                Optional.empty(),
                Optional.empty(),
                false,
                List.of("crop.stage_id == 'mature_clusters'"),
                List.of(InteractionAction.of("HARVEST_CROP")),
                10
        );

        InteractionDefinition harvestShears = new InteractionDefinition(
                Key.of("croppy", "harvest_grapes_shears"),
                Optional.empty(),
                Optional.of(grapesKey),
                InteractionTrigger.RIGHT_CLICK,
                Optional.empty(),
                Optional.of("minecraft:shears"),
                false,
                List.of("crop.is_mature == true"),
                List.of(InteractionAction.of("HARVEST_CROP")),
                20
        );

        InteractionDefinition dismantleAxe = new InteractionDefinition(
                Key.of("croppy", "dismantle_grapes_axe"),
                Optional.empty(),
                Optional.of(grapesKey),
                InteractionTrigger.LEFT_CLICK,
                Optional.empty(),
                Optional.of("tag:axes"),
                false,
                List.of(),
                List.of(InteractionAction.of("DISMANTLE_CROP")),
                30
        );

        InteractionDefinition dismantleSneak = new InteractionDefinition(
                Key.of("croppy", "dismantle_grapes_sneak"),
                Optional.empty(),
                Optional.of(grapesKey),
                InteractionTrigger.SNEAK_LEFT_CLICK,
                Optional.empty(),
                Optional.empty(),
                false,
                List.of(),
                List.of(InteractionAction.of("DISMANTLE_CROP")),
                25
        );

        registry.register(harvestHand);
        registry.register(harvestShears);
        registry.register(dismantleAxe);
        registry.register(dismantleSneak);

        EvaluationContext matureCtx = EvaluationContext.empty()
                .with("crop.stage_id", "mature_clusters")
                .with("crop.is_mature", true)
                .with("crop.height", 2);

        EvaluationContext immatureCtx = EvaluationContext.empty()
                .with("crop.stage_id", "flowering_vine")
                .with("crop.is_mature", false)
                .with("crop.height", 2);

        // 1. Right click with empty hand on mature crop
        Optional<InteractionDefinition> matchHand = router.matchCrop(
                grapesKey,
                InteractionTrigger.RIGHT_CLICK,
                null,
                null,
                true,
                matureCtx
        );
        assertTrue(matchHand.isPresent());
        assertEquals("croppy:harvest_grapes_hand", matchHand.get().key().asString());

        // 2. Right click with empty hand on immature crop -> no match
        Optional<InteractionDefinition> matchImmature = router.matchCrop(
                grapesKey,
                InteractionTrigger.RIGHT_CLICK,
                null,
                null,
                true,
                immatureCtx
        );
        assertTrue(matchImmature.isEmpty());

        // 3. Right click with shears on mature crop -> priority 20 beats priority 10
        Optional<InteractionDefinition> matchShears = router.matchCrop(
                grapesKey,
                InteractionTrigger.RIGHT_CLICK,
                null,
                "minecraft:shears",
                false,
                matureCtx
        );
        assertTrue(matchShears.isPresent());
        assertEquals("croppy:harvest_grapes_shears", matchShears.get().key().asString());

        // 4. Left click with diamond axe -> tag:axes matches
        Optional<InteractionDefinition> matchAxe = router.matchCrop(
                grapesKey,
                InteractionTrigger.LEFT_CLICK,
                null,
                "minecraft:diamond_axe",
                false,
                matureCtx
        );
        assertTrue(matchAxe.isPresent());
        assertEquals("croppy:dismantle_grapes_axe", matchAxe.get().key().asString());

        // 5. Shift left click with empty hand -> SNEAK_LEFT_CLICK matches
        Optional<InteractionDefinition> matchSneak = router.matchCrop(
                grapesKey,
                InteractionTrigger.SNEAK_LEFT_CLICK,
                null,
                null,
                true,
                immatureCtx
        );
        assertTrue(matchSneak.isPresent());
        assertEquals("croppy:dismantle_grapes_sneak", matchSneak.get().key().asString());
    }
}
