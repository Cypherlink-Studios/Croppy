package dev.darkblade.croppy.platform.paper.station.gui;

import dev.darkblade.croppy.api.station.StationInstance;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

/**
 * Custom {@link InventoryHolder} binding a Bukkit inventory to a runtime {@link StationInstance}.
 */
public final class StationGuiHolder implements InventoryHolder {

    private final StationInstance station;
    private final Inventory inventory;

    public StationGuiHolder(StationInstance station) {
        this.station = Objects.requireNonNull(station, "station cannot be null");
        String title = station.getDefinition().displayName();
        this.inventory = Bukkit.createInventory(this, 27, MiniMessage.miniMessage().deserialize(title));
    }

    public StationInstance getStation() {
        return station;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
