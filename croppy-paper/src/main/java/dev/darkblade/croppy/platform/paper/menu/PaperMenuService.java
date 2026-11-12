package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.JulCroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.menu.MenuRegistry;
import dev.darkblade.croppy.api.process.ProcessRegistry;
import dev.darkblade.croppy.api.station.StationDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.platform.paper.expression.PlaceholderApiVariableResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing opening, rendering, and lifecycle of custom menus for players.
 */
public final class PaperMenuService {

    private final MenuRegistry menuRegistry;
    private final PaperMenuRenderer renderer;
    private final ProcessRegistry processRegistry;
    private final PaperMenuTicker ticker;
    private final CroppyLogger logger;

    public PaperMenuService(MenuRegistry menuRegistry, PaperMenuRenderer renderer, ProcessRegistry processRegistry, PaperMenuTicker ticker, Logger logger) {
        this(menuRegistry, renderer, processRegistry, ticker, logger != null ? new JulCroppyLogger(logger) : NoOpCroppyLogger.INSTANCE);
    }

    public PaperMenuService(MenuRegistry menuRegistry, PaperMenuRenderer renderer, ProcessRegistry processRegistry, PaperMenuTicker ticker, CroppyLogger logger) {
        this.menuRegistry = Objects.requireNonNull(menuRegistry, "menuRegistry cannot be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        this.processRegistry = processRegistry;
        this.ticker = Objects.requireNonNull(ticker, "ticker cannot be null");
        this.logger = (logger != null) ? logger : NoOpCroppyLogger.INSTANCE;
    }

    /**
     * Opens the configured station menu for a player.
     *
     * @param player  the viewing player
     * @param station the station instance
     */
    public void openStationMenu(Player player, StationInstance station) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(station, "station cannot be null");

        Key menuKey = station.getDefinition().menuKey();
        Optional<MenuDefinition> menuOpt = menuRegistry.get(menuKey);

        if (menuOpt.isEmpty()) {
            menuOpt = menuRegistry.get(StationDefinition.DEFAULT_MENU_KEY);
            if (menuOpt.isEmpty()) {
                logger.warn(LogChannel.STATION, "Could not find menu definition '" + menuKey + "' nor default '" + StationDefinition.DEFAULT_MENU_KEY + "' for station " + station.getStationKey());
                return;
            }
        }

        MenuDefinition menu = menuOpt.get();

        EvaluationContext baseCtx = EvaluationContext.empty().with("player", player);
        if (PlaceholderApiVariableResolver.isAvailable()) {
            baseCtx = baseCtx.withResolver(new PlaceholderApiVariableResolver());
        }

        EvaluationContext context = ticker.enrichContext(baseCtx, station, player);
        PaperMenuHolder holder = new PaperMenuHolder(menu, station, context);
        Inventory inv = renderer.createInventory(holder);
        player.openInventory(inv);
    }

    /**
     * Opens an arbitrary registered menu for a player.
     *
     * @param player  the viewing player
     * @param menuKey menu definition key
     * @param context initial evaluation context
     */
    public void openMenu(Player player, Key menuKey, EvaluationContext context) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(menuKey, "menuKey cannot be null");

        Optional<MenuDefinition> menuOpt = menuRegistry.get(menuKey);
        if (menuOpt.isEmpty()) {
            logger.warn(LogChannel.STATION, "Menu definition not found: " + menuKey);
            return;
        }

        EvaluationContext baseCtx = (context == null) ? EvaluationContext.empty() : context;
        baseCtx = baseCtx.with("player", player);
        if (PlaceholderApiVariableResolver.isAvailable()) {
            baseCtx = baseCtx.withResolver(new PlaceholderApiVariableResolver());
        }

        PaperMenuHolder holder = new PaperMenuHolder(menuOpt.get(), null, baseCtx);
        Inventory inv = renderer.createInventory(holder);
        player.openInventory(inv);
    }

    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    public PaperMenuRenderer getRenderer() {
        return renderer;
    }

    public PaperMenuTicker getTicker() {
        return ticker;
    }
}
