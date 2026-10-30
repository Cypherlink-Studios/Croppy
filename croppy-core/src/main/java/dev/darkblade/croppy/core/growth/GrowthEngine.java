package dev.darkblade.croppy.core.growth;

import dev.darkblade.croppy.api.crop.CropInstance;
import dev.darkblade.croppy.api.growth.CropEnvironment;
import dev.darkblade.croppy.api.growth.DisplaySynchronizer;
import dev.darkblade.croppy.api.growth.GrowthContext;
import dev.darkblade.croppy.api.growth.GrowthModifier;
import dev.darkblade.croppy.api.growth.StageAdvanceInterceptor;
import dev.darkblade.croppy.api.growth.UnwateredPolicy;
import dev.darkblade.croppy.api.growth.WitherHandler;
import dev.darkblade.croppy.api.registry.Service;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service orchestrating deterministic agricultural growth simulation, condition checks,
 * modifier pipeline calculation, hydration/unwatered policy execution, and stage progression.
 */
public class GrowthEngine implements Service {

    private double baseProgress = 1.0;
    private final List<GrowthModifier> modifiers = new CopyOnWriteArrayList<>();
    private DisplaySynchronizer displaySynchronizer;
    private StageAdvanceInterceptor stageAdvanceInterceptor = (c, s, from, to) -> true;
    private WitherHandler witherHandler;

    public GrowthEngine() {}

    public GrowthEngine(double baseProgress) {
        setBaseProgress(baseProgress);
    }

    public double baseProgress() {
        return baseProgress;
    }

    public void setBaseProgress(double baseProgress) {
        if (baseProgress < 0.0) {
            throw new IllegalArgumentException("baseProgress must not be negative");
        }
        this.baseProgress = baseProgress;
    }

    public void addModifier(GrowthModifier modifier) {
        Objects.requireNonNull(modifier, "modifier must not be null");
        this.modifiers.add(modifier);
    }

    public boolean removeModifier(GrowthModifier modifier) {
        return this.modifiers.remove(modifier);
    }

    public void clearModifiers() {
        this.modifiers.clear();
    }

    public List<GrowthModifier> modifiers() {
        return Collections.unmodifiableList(this.modifiers);
    }

    public DisplaySynchronizer displaySynchronizer() {
        return displaySynchronizer;
    }

    public void setDisplaySynchronizer(DisplaySynchronizer displaySynchronizer) {
        this.displaySynchronizer = displaySynchronizer;
    }

    public StageAdvanceInterceptor stageAdvanceInterceptor() {
        return stageAdvanceInterceptor;
    }

    public void setStageAdvanceInterceptor(StageAdvanceInterceptor stageAdvanceInterceptor) {
        this.stageAdvanceInterceptor = Objects.requireNonNull(stageAdvanceInterceptor, "stageAdvanceInterceptor must not be null");
    }

    public WitherHandler witherHandler() {
        return witherHandler;
    }

    public void setWitherHandler(WitherHandler witherHandler) {
        this.witherHandler = witherHandler;
    }

    /**
     * Checks if the ambient environment satisfies the requirements for the given species.
     */
    public boolean isEnvironmentSuitable(Species species, CropEnvironment environment) {
        if (!environment.isLoaded()) {
            return false;
        }

        CropRequirements requirements = species.getRequirements();
        if (environment.lightLevel() < requirements.minLight()) {
            return false;
        }

        if (requirements.requiresWater() && environment.waterFluidLevel() < requirements.waterFluidLevel()) {
            return false;
        }

        return true;
    }

    /**
     * Evaluates a crop instance for growth progress without day-based hydration checks.
     */
    public GrowthResult processCrop(CropInstance crop, Species species, CropEnvironment environment) {
        return processCrop(crop, species, environment, -1L);
    }

    /**
     * Evaluates a crop instance for growth progress in the current cycle, taking current world day into account.
     */
    public GrowthResult processCrop(CropInstance crop, Species species, CropEnvironment environment, long currentDay) {
        Objects.requireNonNull(crop, "crop must not be null");
        Objects.requireNonNull(species, "species must not be null");
        Objects.requireNonNull(environment, "environment must not be null");

        // Skip unloaded locations or already mature crops
        if (!environment.isLoaded() || crop.isMature(species)) {
            return GrowthResult.skipped();
        }

        // Verify environmental requirements (light, water fluid level)
        if (!isEnvironmentSuitable(species, environment)) {
            return GrowthResult.requirementsNotMet();
        }

        // Check daily hydration if day tracking is enabled (currentDay >= 0)
        boolean isWatered = (currentDay < 0) || crop.isWateredOn(currentDay);
        if (!isWatered) {
            return handleUnwateredCrop(crop, species, currentDay);
        }

        // Standard growth with modifier chain
        GrowthContext context = new GrowthContext(crop, species, environment, this.baseProgress);
        double effectiveProgress = this.baseProgress;
        for (GrowthModifier modifier : this.modifiers) {
            effectiveProgress *= modifier.calculateModifier(context);
        }

        if (effectiveProgress < 0.0) {
            effectiveProgress = 0.0;
        }

        crop.addProgress(effectiveProgress);
        boolean advanced = checkStageAdvance(crop, species);

        return GrowthResult.progressed(effectiveProgress, advanced);
    }

    private GrowthResult handleUnwateredCrop(CropInstance crop, Species species, long currentDay) {
        UnwateredPolicy policy = species.getUnwateredPolicy();
        if (policy == null) {
            policy = UnwateredPolicy.pause();
        }

        return switch (policy.type()) {
            case PAUSE -> GrowthResult.progressed(0.0, false);
            case SLOW -> {
                double effectiveProgress = this.baseProgress * policy.slowRateMultiplier();
                for (GrowthModifier modifier : this.modifiers) {
                    effectiveProgress *= modifier.calculateModifier(new GrowthContext(crop, species, CropEnvironment.of(15, 8), this.baseProgress));
                }
                if (effectiveProgress < 0.0) {
                    effectiveProgress = 0.0;
                }
                crop.addProgress(effectiveProgress);
                boolean advanced = checkStageAdvance(crop, species);
                yield GrowthResult.progressed(effectiveProgress, advanced);
            }
            case DECAY_PROGRESS -> {
                double decay = policy.decayAmount();
                double newProgress = Math.max(0.0, crop.getProgress() - decay);
                crop.setProgress(newProgress);
                yield GrowthResult.progressed(-decay, false);
            }
            case WITHER -> {
                long lastWatered = crop.getLastWateredDay();
                long dryDays = (lastWatered >= 0) ? (currentDay - lastWatered) : (currentDay + 1);
                if (dryDays > policy.maxDryDays()) {
                    if (this.witherHandler != null) {
                        this.witherHandler.onWither(crop, species);
                    }
                    yield GrowthResult.ofWithered();
                }
                yield GrowthResult.progressed(0.0, false);
            }
        };
    }

    private boolean checkStageAdvance(CropInstance crop, Species species) {
        int currentStageIndex = crop.getStageIndex();
        StageDefinition currentStage = species.getStage(currentStageIndex);
        boolean advanced = false;

        if (crop.getProgress() >= currentStage.progressRequired() && !crop.isMature(species)) {
            int nextStageIndex = currentStageIndex + 1;
            boolean permitted = this.stageAdvanceInterceptor.shouldAdvance(crop, species, currentStageIndex, nextStageIndex);

            if (permitted) {
                crop.advanceStage();
                advanced = true;

                if (this.displaySynchronizer != null) {
                    StageDefinition newStage = species.getStage(crop.getStageIndex());
                    this.displaySynchronizer.synchronizeDisplay(crop, species, newStage);
                }
            } else {
                // Event vetoed/cancelled: cap progress at the threshold
                crop.setProgress(currentStage.progressRequired());
            }
        }
        return advanced;
    }

    public void shutdown() {
        this.modifiers.clear();
        this.displaySynchronizer = null;
        this.witherHandler = null;
    }
}
