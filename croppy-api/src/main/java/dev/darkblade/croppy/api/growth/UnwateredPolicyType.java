package dev.darkblade.croppy.api.growth;

/**
 * Defines the policy applied to a crop when it lacks daily soil hydration.
 */
public enum UnwateredPolicyType {
    /**
     * Halts progress accumulation completely for the day without damage.
     */
    PAUSE,

    /**
     * Allows progress accumulation at a reduced rate.
     */
    SLOW,

    /**
     * Deducts progress points from current stage accumulation.
     */
    DECAY_PROGRESS,

    /**
     * Destroys or withers the crop if unwatered for more than maxDryDays consecutive days.
     */
    WITHER
}
