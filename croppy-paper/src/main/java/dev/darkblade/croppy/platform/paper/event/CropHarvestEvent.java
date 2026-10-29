package dev.darkblade.croppy.platform.paper.event;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fired when a mature crop is harvested by a player.
 */
public class CropHarvestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Species species;
    private final Block block;
    private final CropInstance cropInstance;
    private final List<ItemStack> drops;
    private boolean cancelled;

    public CropHarvestEvent(@Nullable Player player, @NotNull Species species, @NotNull Block block, @NotNull CropInstance cropInstance, @NotNull List<ItemStack> drops) {
        this.player = player;
        this.species = species;
        this.block = block;
        this.cropInstance = cropInstance;
        this.drops = drops;
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

    @NotNull
    public List<ItemStack> getDrops() {
        return drops;
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
