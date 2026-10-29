package dev.darkblade.croppy.platform.paper.event;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a crop instance is about to advance to its next developmental stage.
 * Can be cancelled by external listeners to prevent the stage advancement.
 */
public class CropGrowEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CropInstance cropInstance;
    private final Species species;
    private final Block block;
    private final int fromStageIndex;
    private final int toStageIndex;
    private final StageDefinition previousStage;
    private final StageDefinition nextStage;
    private boolean cancelled;

    public CropGrowEvent(@NotNull CropInstance cropInstance,
                         @NotNull Species species,
                         @NotNull Block block,
                         int fromStageIndex,
                         int toStageIndex,
                         @NotNull StageDefinition previousStage,
                         @NotNull StageDefinition nextStage) {
        this.cropInstance = cropInstance;
        this.species = species;
        this.block = block;
        this.fromStageIndex = fromStageIndex;
        this.toStageIndex = toStageIndex;
        this.previousStage = previousStage;
        this.nextStage = nextStage;
    }

    @NotNull
    public CropInstance getCropInstance() {
        return cropInstance;
    }

    @NotNull
    public Species getSpecies() {
        return species;
    }

    @NotNull
    public Block getBlock() {
        return block;
    }

    public int getFromStageIndex() {
        return fromStageIndex;
    }

    public int getToStageIndex() {
        return toStageIndex;
    }

    @NotNull
    public StageDefinition getPreviousStage() {
        return previousStage;
    }

    @NotNull
    public StageDefinition getNextStage() {
        return nextStage;
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
