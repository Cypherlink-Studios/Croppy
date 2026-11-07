package dev.darkblade.croppy.core.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import dev.darkblade.croppy.api.addon.AddonState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAddonManagerCollisionTest {

    @TempDir
    Path addonsDir;

    private DefaultAddonManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultAddonManager(addonsDir, null);
    }

    @Test
    @DisplayName("Reject duplicate addon identifier")
    void testDuplicateIdentifierRejection() {
        AddonDescription desc1 = new AddonDescription(
                "coffee", "Coffee 1", "1.0", "coffee", Optional.empty(), List.of(), "", List.of(), List.of()
        );
        AddonDescription desc2 = new AddonDescription(
                "coffee", "Coffee 2", "2.0", "coffee", Optional.empty(), List.of(), "", List.of(), List.of()
        );

        Path path1 = addonsDir.resolve("coffee-1.jar");
        Path path2 = addonsDir.resolve("coffee-2.jar");

        assertTrue(manager.registerDiscovered(desc1, path1));
        assertFalse(manager.registerDiscovered(desc2, path2));

        assertEquals("Coffee 1", manager.getAddon("coffee").orElseThrow().name());
    }

    @Test
    @DisplayName("Directory addon binds root folder as data folder")
    void testDirectoryAddonDataFolder() throws IOException {
        Path dir = addonsDir.resolve("my-pack");
        Files.createDirectories(dir);

        AddonDescription desc = new AddonDescription(
                "my-pack", "My Pack", "1.0", "my_pack", Optional.empty(), List.of(), "", List.of(), List.of()
        );

        assertTrue(manager.registerDiscovered(desc, dir));
        assertEquals(dir, manager.getDataFolder("my-pack").orElseThrow());
    }

    @Test
    @DisplayName("Halt loading when JAR addon data folder collides with existing directory addon root")
    void testCrossOccupancyCollisionHalt() throws IOException {
        // Directory addon 'winter' is located at addons/frost/
        Path frostDir = addonsDir.resolve("frost");
        Files.createDirectories(frostDir);

        AddonDescription dirAddon = new AddonDescription(
                "winter", "Winter Pack", "1.0", "winter", Optional.empty(), List.of(), "", List.of(), List.of()
        );
        assertTrue(manager.registerDiscovered(dirAddon, frostDir));

        // JAR addon 'frost' targets data folder addons/frost/
        Path frostJar = addonsDir.resolve("frost.jar");
        Files.createFile(frostJar);

        AddonDescription jarAddon = new AddonDescription(
                "frost", "Frost Addon", "1.0", "frost", Optional.empty(), List.of(), "", List.of(), List.of()
        );
        assertTrue(manager.registerDiscovered(jarAddon, frostJar));

        // Loading 'winter' directory addon succeeds
        assertTrue(manager.loadAddon("winter"));
        assertEquals(AddonState.LOADED, manager.getState("winter"));

        // Loading 'frost' JAR addon must be halted due to Path Ownership Gate collision with 'frost' dir
        assertFalse(manager.loadAddon("frost"));
        assertEquals(AddonState.ON_DISK, manager.getState("frost"));
    }
}
