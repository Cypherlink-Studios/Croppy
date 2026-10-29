package dev.darkblade.croppy.platform.paper;

import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.core.CroppyCore;
import dev.darkblade.croppy.core.species.YamlSpeciesLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Main Paper plugin adapter bridging server lifecycle events to the decoupled core.
 */
public class CroppyPlugin extends JavaPlugin {

    private CroppyCore core;

    @Override
    public void onEnable() {
        getLogger().info("Enabling Croppy plugin on Paper 1.21.8...");
        this.core = new CroppyCore(getLogger());
        try {
            this.core.initialize();
            setupSpeciesDefinitions();
            registerListeners();
            getLogger().info("Croppy enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Croppy: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new dev.darkblade.croppy.platform.paper.listener.CropPlantListener(core), this);
        pm.registerEvents(new dev.darkblade.croppy.platform.paper.listener.CropGrowthListener(core), this);
        pm.registerEvents(new dev.darkblade.croppy.platform.paper.listener.CropBreakListener(core), this);
        pm.registerEvents(new dev.darkblade.croppy.platform.paper.listener.CropPhysicsListener(core), this);
        getLogger().info("Registered crop lifecycle listeners.");
    }

    private void setupSpeciesDefinitions() {
        Path speciesDir = getDataFolder().toPath().resolve("species");
        if (!Files.exists(speciesDir)) {
            saveResource("species/wheat.yml", false);
            saveResource("species/carrot.yml", false);
            saveResource("species/potato.yml", false);
        }

        YamlSpeciesLoader loader = new YamlSpeciesLoader();
        List<Species> loaded = loader.loadDirectory(speciesDir, getLogger());
        for (Species species : loaded) {
            core.getSpeciesRegistry().register(species);
        }
        getLogger().info("Registered " + core.getSpeciesRegistry().size() + " crop species into SpeciesRegistry.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Croppy plugin...");
        if (this.core != null) {
            this.core.shutdown();
        }
        getLogger().info("Croppy disabled cleanly.");
    }

    /**
     * @return the central core domain orchestrator
     */
    public CroppyCore getCore() {
        return core;
    }
}

