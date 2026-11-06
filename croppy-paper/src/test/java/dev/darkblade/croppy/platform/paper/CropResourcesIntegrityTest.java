package dev.darkblade.croppy.platform.paper;

import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.core.interaction.DefaultInteractionRegistry;
import dev.darkblade.croppy.core.interaction.YamlInteractionLoader;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class CropResourcesIntegrityTest {

    @Test
    @DisplayName("Verify all bundled crop definitions parse cleanly including grapes and rice")
    void testBundledCropsIntegrity() {
        Path cropsDir = Paths.get("src", "main", "resources", "crops");
        assertTrue(Files.isDirectory(cropsDir), "crops resource directory must exist");

        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        List<Species> loaded = loader.loadDirectory(cropsDir, Logger.getAnonymousLogger());

        assertFalse(loaded.isEmpty(), "Bundled crops must not be empty");
        assertTrue(loaded.stream().anyMatch(s -> s.getKey().asString().equals("croppy:grapes")), "Must contain croppy:grapes");
        assertTrue(loaded.stream().anyMatch(s -> s.getKey().asString().equals("croppy:rice")), "Must contain croppy:rice");

        // Verify grapes properties
        Species grapes = loaded.stream().filter(s -> s.getKey().asString().equals("croppy:grapes")).findFirst().orElseThrow();
        assertEquals(2, grapes.getHeight());
        assertTrue(grapes.getHarvest().hasRegrowth());
        assertEquals("flowering_vine", grapes.getHarvest().regrowth().orElseThrow().targetStageId());
        assertEquals(6, grapes.getStages().size(), "Grapes should have 6 growth stages (0-5)");
        for (int i = 0; i <= 5; i++) {
            assertNotNull(grapes.getStages().get(i).model3D(), "Stage " + i + " must have a 3D model");
            assertEquals("croppy:plant/grape/stage_" + i, grapes.getStages().get(i).model3D().itemModel().asString());
        }

        // Verify rice properties
        Species rice = loaded.stream().filter(s -> s.getKey().asString().equals("croppy:rice")).findFirst().orElseThrow();
        assertEquals(2, rice.getHeight());
        assertEquals("minecraft:water", rice.getDisplay().blockId());
    }

    @Test
    @DisplayName("Verify all bundled interaction definitions load cleanly including crop interactions")
    void testBundledInteractionsIntegrity() {
        Path interactionsDir = Paths.get("src", "main", "resources", "interactions");
        assertTrue(Files.isDirectory(interactionsDir), "interactions resource directory must exist");

        DefaultInteractionRegistry registry = new DefaultInteractionRegistry();
        YamlInteractionLoader loader = new YamlInteractionLoader();
        int count = loader.loadAll(interactionsDir, registry, Logger.getAnonymousLogger());

        assertTrue(count > 0, "Must load interactions");
        assertFalse(registry.getInteractionsForCrop(dev.darkblade.croppy.api.identifier.Key.of("croppy", "grapes")).isEmpty(), "Must have grape interactions");
        assertFalse(registry.getInteractionsForCrop(dev.darkblade.croppy.api.identifier.Key.of("croppy", "rice")).isEmpty(), "Must have rice interactions");
    }
}
