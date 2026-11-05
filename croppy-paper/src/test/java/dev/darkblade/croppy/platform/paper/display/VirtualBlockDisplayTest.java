package dev.darkblade.croppy.platform.paper.display;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.Model3DDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import dev.darkblade.croppy.platform.paper.station.display.VirtualEntityIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VirtualBlockDisplayTest {

    @BeforeEach
    void setUp() {
        VirtualEntityIdGenerator.reset();
    }

    @Test
    @DisplayName("Allocate unique negative entity IDs for ItemDisplay and Interaction")
    void testEntityIdsAllocated() {
        BlockPosition pos = BlockPosition.of("world", 100, 64, 200);
        Model3DDefinition model = Model3DDefinition.of(Key.croppy("tomato_stage_0"), 0.6f, 0.4f);
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 90.0f, model);

        int itemId = display.getItemDisplayId();
        int interactionId = display.getInteractionId();

        assertTrue(itemId < 0, "ItemDisplay ID must be negative");
        assertTrue(interactionId < 0, "Interaction ID must be negative");
        assertNotEquals(itemId, interactionId, "IDs must not collide");

        assertNotNull(display.getItemDisplayUuid());
        assertNotNull(display.getInteractionUuid());
        assertNotEquals(display.getItemDisplayUuid(), display.getInteractionUuid());

        int[] allIds = display.getAllEntityIds();
        assertEquals(2, allIds.length);
        assertEquals(itemId, allIds[0]);
        assertEquals(interactionId, allIds[1]);

        assertEquals(pos, display.getPosition());
        assertEquals(90.0f, display.getYaw(), 0.001f);
        assertEquals(model, display.getModel());
    }

    @Test
    @DisplayName("Track player viewers correctly")
    void testViewerTracking() {
        BlockPosition pos = BlockPosition.of("world", 50, 70, 50);
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 0.0f);

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        assertTrue(display.addViewer(player1));
        assertFalse(display.addViewer(player1)); // duplicate should return false
        assertTrue(display.isViewing(player1));
        assertFalse(display.isViewing(player2));

        assertTrue(display.addViewer(player2));
        assertEquals(2, display.getViewers().size());

        assertTrue(display.removeViewer(player1));
        assertFalse(display.isViewing(player1));
        assertTrue(display.isViewing(player2));

        display.clearViewers();
        assertTrue(display.getViewers().isEmpty());
    }

    @Test
    @DisplayName("Update yaw and model definition dynamically")
    void testStateUpdates() {
        BlockPosition pos = BlockPosition.of("world", 0, 64, 0);
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 45.0f);

        assertEquals(45.0f, display.getYaw(), 0.001f);
        assertNull(display.getModel());

        display.setYaw(180.0f);
        assertEquals(180.0f, display.getYaw(), 0.001f);

        Model3DDefinition nextModel = Model3DDefinition.of(Key.croppy("tomato_stage_1"), 0.8f, 0.9f);
        display.setModel(nextModel);
        assertEquals(nextModel, display.getModel());
    }

    @Test
    @DisplayName("Dynamically adjust vertical multi-block hitbox dimensions for trellis crops")
    void testTrellisMultiBlockHitboxTransition() {
        BlockPosition pos = BlockPosition.of("world", 10, 64, 20);
        Model3DDefinition shootModel = Model3DDefinition.of(Key.croppy("grape_stage_0"), 0.4f, 0.4f);
        VirtualBlockDisplay display = new VirtualBlockDisplay(pos, 0.0f, shootModel);

        assertEquals(0.4f, display.getModel().hitboxHeight(), 0.001f);

        // Advance to multi-block trellis stage
        Model3DDefinition trellisMature = Model3DDefinition.of(Key.croppy("grape_vine_mature"), 0.8f, 2.0f);
        display.setModel(trellisMature);

        assertEquals(2.0f, display.getModel().hitboxHeight(), 0.001f);
        assertEquals(0.8f, display.getModel().hitboxWidth(), 0.001f);

        // Regrowth back to flowering vine
        Model3DDefinition floweringModel = Model3DDefinition.of(Key.croppy("grape_vine_flowering"), 0.8f, 2.0f);
        display.setModel(floweringModel);
        assertEquals(2.0f, display.getModel().hitboxHeight(), 0.001f);
    }
}
