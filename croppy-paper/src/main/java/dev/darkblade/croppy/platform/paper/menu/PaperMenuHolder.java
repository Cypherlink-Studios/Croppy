package dev.darkblade.croppy.platform.paper.menu;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.menu.MenuDefinition;
import dev.darkblade.croppy.api.station.StationInstance;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;
import java.util.Optional;

/**
 * Custom {@link InventoryHolder} binding a Bukkit {@link Inventory} to a {@link MenuDefinition}
 * and runtime context (such as an optional {@link StationInstance}).
 */
public final class PaperMenuHolder implements InventoryHolder {

    private final MenuDefinition menu;
    private final StationInstance station;
    private EvaluationContext context;
    private Inventory inventory;

    public PaperMenuHolder(MenuDefinition menu, StationInstance station, EvaluationContext context) {
        this.menu = Objects.requireNonNull(menu, "menu cannot be null");
        this.station = station; // may be null for general non-station menus
        this.context = (context == null) ? EvaluationContext.empty() : context;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public MenuDefinition getMenu() {
        return menu;
    }

    public Optional<StationInstance> getStation() {
        return Optional.ofNullable(station);
    }

    public EvaluationContext getContext() {
        return context;
    }

    public void setContext(EvaluationContext context) {
        this.context = (context == null) ? EvaluationContext.empty() : context;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
