package dev.darkblade.croppy.platform.paper.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.api.world.BlockPosition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Paper implementation of DisplaySynchronizer.
 * Synchronizes physical world block states to match crop developmental stages.
 */
public class PaperDisplaySynchronizer implements DisplaySynchronizer {

    private final Server server;
    private final Logger logger;

    public PaperDisplaySynchronizer(Server server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    public PaperDisplaySynchronizer(Server server) {
        this(server, server.getLogger());
    }

    @Override
    public void synchronizeDisplay(CropInstance crop, Species species, StageDefinition stage) {
        Objects.requireNonNull(crop, "crop must not be null");
        Objects.requireNonNull(species, "species must not be null");
        Objects.requireNonNull(stage, "stage must not be null");

        BlockPosition pos = crop.getPosition();
        World world = server.getWorld(pos.worldName());
        if (world == null) {
            return;
        }

        int chunkX = pos.x() >> 4;
        int chunkZ = pos.z() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
        CropDisplay display = species.getDisplay();

        if ("vanilla".equalsIgnoreCase(display.type())) {
            synchronizeVanillaDisplay(block, display, stage);
        }
    }

    private void synchronizeVanillaDisplay(Block block, CropDisplay display, StageDefinition stage) {
        Material expectedMaterial = resolveMaterial(display.blockId());
        if (expectedMaterial == null) {
            return;
        }

        if (block.getType() != expectedMaterial) {
            block.setType(expectedMaterial, false);
        }

        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            int targetAge = Math.min(stage.blockAge(), ageable.getMaximumAge());
            ageable.setAge(targetAge);
            block.setBlockData(ageable, false);
        }
    }

    private Material resolveMaterial(String id) {
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key != null) {
            Material material = Registry.MATERIAL.get(key);
            if (material != null) {
                return material;
            }
        }
        return Material.matchMaterial(id);
    }
}
