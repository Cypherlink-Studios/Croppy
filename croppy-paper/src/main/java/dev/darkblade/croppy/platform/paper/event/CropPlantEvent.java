package dev.darkblade.croppy.platform.paper.event;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a crop is planted by a player.
 */
public class CropPlantEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Species species;
    private final Block block;
    private final CropInstance cropInstance;
    private boolean cancelled;

    public CropPlantEvent(@Nullable Player player, @NotNull Species species, @NotNull Block block, @NotNull CropInstance cropInstance) {
        this.player = player;
        this.species = species;
        this.block = block;
        this.cropInstance = cropInstance;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Species getSpecies() {
        return species;
    }

    @NotNull
    public Block getBlock() {
        return block;
    }

    @NotNull
    public CropInstance getCropInstance() {
        return cropInstance;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
