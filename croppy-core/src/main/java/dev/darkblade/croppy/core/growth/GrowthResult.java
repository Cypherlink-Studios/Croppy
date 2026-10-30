package dev.darkblade.croppy.core.growth;

/**
 * Outcome of evaluating a single crop instance in a growth cycle.
 *
 * @param evaluated whether evaluation took place (false if skipped, e.g. chunk unloaded or already mature)
 * @param requirementsMet whether ambient conditions (light, water) met species requirements
 * @param progressAdded the amount of progress gained in this cycle
 * @param stageAdvanced whether the crop advanced to a subsequent developmental stage
 * @param withered whether the crop withered due to unwatered policy
 */
public record GrowthResult(
        boolean evaluated,
        boolean requirementsMet,
        double progressAdded,
        boolean stageAdvanced,
        boolean withered
) {
    public GrowthResult(boolean evaluated, boolean requirementsMet, double progressAdded, boolean stageAdvanced) {
        this(evaluated, requirementsMet, progressAdded, stageAdvanced, false);
    }

    public static GrowthResult skipped() {
        return new GrowthResult(false, false, 0.0, false, false);
    }

    public static GrowthResult requirementsNotMet() {
        return new GrowthResult(true, false, 0.0, false, false);
    }

    public static GrowthResult progressed(double added, boolean advanced) {
        return new GrowthResult(true, true, added, advanced, false);
    }

    public static GrowthResult ofWithered() {
        return new GrowthResult(true, true, 0.0, false, true);
    }
}
