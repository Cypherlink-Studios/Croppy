package dev.darkblade.croppy.platform.paper;

import dev.darkblade.croppy.core.CroppyCore;
import org.bukkit.plugin.java.JavaPlugin;

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
            getLogger().info("Croppy enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Croppy core: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
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
