package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.addon.CroppyAddon;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.item.ItemDefinition;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.RotationMode;
import dev.darkblade.croppy.api.species.SpatialFootprint;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.core.CroppyCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AddonDecommissionerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Decommission executes 5-phase cleanup and purges domain registries")
    void testDecommissionPhases() throws Exception {
        AddonDescription desc = new AddonDescription(
                "test-addon", "Test Addon", "1.0", "test_addon", Optional.of("dummy.Main"), List.of(), "", List.of(), List.of()
        );

        CroppyCore core = new CroppyCore();
        core.initialize();

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, null, null, core);

        // Register domain items and species under addon
        Key itemKey = Key.of("test_addon", "wand");
        ItemDefinition item = new ItemDefinition(itemKey, "tools", "minecraft:stick", 0, null, "Wand", List.of(), Map.of());
        core.getItemRegistry().register(item);
        context.trackItem(itemKey);

        Key speciesKey = Key.of("test_addon", "moon_crop");
        Species species = new Species() {
            @Override public Key getKey() { return speciesKey; }
            @Override public String getName() { return "Moon Crop"; }
            @Override public dev.darkblade.croppy.api.species.CropDisplay getDisplay() {
                return new dev.darkblade.croppy.api.species.CropDisplay("vanilla", "minecraft:wheat", "minecraft:wheat_seeds");
            }
            @Override public List<dev.darkblade.croppy.api.species.StageDefinition> getStages() { return List.of(); }
            @Override public dev.darkblade.croppy.api.species.CropRequirements getRequirements() { return new dev.darkblade.croppy.api.species.CropRequirements(0, 0); }
            @Override public dev.darkblade.croppy.api.species.HarvestDefinition getHarvest() { return null; }
        };
        core.getSpeciesRegistry().register(species);
        context.trackSpecies(speciesKey);

        Key menuKey = Key.of("test_addon", "custom_menu");
        MenuDefinition menu = new MenuDefinition(
                menuKey,
                "Custom Menu",
                1,
                List.of("123456789"),
                Map.of(),
                Map.of()
        );
        core.getMenuRegistry().register(menu);
        context.trackMenu(menuKey);

        GrowthModifier modifier = ctx -> 1.5;
        core.getGrowthEngine().addModifier(modifier);
        context.trackGrowthModifier(modifier);

        AtomicBoolean onDisableCalled = new AtomicBoolean(false);
        CroppyAddon addon = new CroppyAddon() {
            @Override
            public void onEnable(dev.darkblade.croppy.api.addon.AddonContext ctx) {}

            @Override
            public void onDisable(dev.darkblade.croppy.api.addon.AddonContext ctx) {
                onDisableCalled.set(true);
            }
        };

        AddonClassLoader classLoader = new AddonClassLoader(new URL[0], ClassLoader.getPlatformClassLoader(), desc);

        // Execute decommission with isUnload = true
        AddonDecommissioner.decommission(addon, context, classLoader, core, null, true);

        // Phase 1 verification
        assertTrue(onDisableCalled.get());

        // Phase 3 verification
        assertTrue(context.getManagedExecutor().isShutdown());

        // Phase 4 verification
        assertFalse(core.getItemRegistry().contains(itemKey));
        assertFalse(core.getSpeciesRegistry().contains(speciesKey));
        assertFalse(core.getMenuRegistry().contains(menuKey));

        // Phase 5 verification: classloader closed
        assertTrue(classLoader.getDependencyLoaders().isEmpty());
        assertThrows(ClassNotFoundException.class, () -> classLoader.findClassLocally("com.example.Test"));
    }

    @Test
    @DisplayName("Decommissioning continues gracefully if onDisable throws exception")
    void testFaultTolerantDecommission() {
        AddonDescription desc = new AddonDescription(
                "faulty-addon", "Faulty Addon", "1.0", "faulty", Optional.of("dummy.Main"), List.of(), "", List.of(), List.of()
        );

        CroppyCore core = new CroppyCore();
        core.initialize();

        PaperAddonContext context = new PaperAddonContext(desc, tempDir, null, null, core);
        Key itemKey = Key.of("faulty", "bad_item");
        ItemDefinition item = new ItemDefinition(itemKey, "misc", "minecraft:dirt", 0, null, "Bad", List.of(), Map.of());
        core.getItemRegistry().register(item);
        context.trackItem(itemKey);

        CroppyAddon faultyAddon = new CroppyAddon() {
            @Override
            public void onEnable(dev.darkblade.croppy.api.addon.AddonContext ctx) {}

            @Override
            public void onDisable(dev.darkblade.croppy.api.addon.AddonContext ctx) {
                throw new RuntimeException("Simulated onDisable failure");
            }
        };

        // Should not rethrow exception, but proceed to subsequent phases
        assertDoesNotThrow(() ->
                AddonDecommissioner.decommission(faultyAddon, context, null, core, null, false));

        // Registry should still be purged
        assertFalse(core.getItemRegistry().contains(itemKey));
    }
}
