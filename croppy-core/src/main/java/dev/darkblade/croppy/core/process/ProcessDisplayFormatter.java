package dev.darkblade.croppy.core.process;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.process.PhaseDefinition;
import dev.darkblade.croppy.api.process.PhaseDisplay;
import dev.darkblade.croppy.api.process.ProcessDefinition;
import dev.darkblade.croppy.api.process.ProcessInstance;

import java.util.Objects;

/**
 * High-performance placeholder resolver formatting station display holograms
 * and status lines during process simulation.
 */
public final class ProcessDisplayFormatter {

    /**
     * Formats the phase's configured display template using active process metrics.
     *
     * @param phase      the active phase definition
     * @param processDef the parent process definition (optional)
     * @param instance   the active process runtime instance
     * @param context    the environmental evaluation context
     * @return MiniMessage formatted status text with all placeholders resolved
     */
    public String format(PhaseDefinition phase, ProcessDefinition processDef, ProcessInstance instance, EvaluationContext context) {
        Objects.requireNonNull(phase, "phase cannot be null");
        Objects.requireNonNull(instance, "instance cannot be null");
        PhaseDisplay display = phase.display() != null ? phase.display() : PhaseDisplay.DEFAULT;
        return format(display.template(), phase, display, processDef, instance, context);
    }

    /**
     * Formats an arbitrary template string using phase and process runtime values.
     *
     * @param template   MiniMessage template string with placeholders
     * @param phase      active phase definition
     * @param display    display settings (for bar length/characters)
     * @param processDef parent process definition (optional)
     * @param instance   process instance
     * @param context    evaluation context
     * @return formatted string
     */
    public String format(
            String template,
            PhaseDefinition phase,
            PhaseDisplay display,
            ProcessDefinition processDef,
            ProcessInstance instance,
            EvaluationContext context
    ) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        long elapsedTicks = instance.getPhaseElapsedTicks();
        long targetTicks = phase.durationTicks();
        if (targetTicks <= 0 && phase.timeoutTicks() > 0) {
            targetTicks = phase.timeoutTicks();
        }

        int percent = 0;
        long ticksLeft = 0;
        long secondsElapsed = elapsedTicks / 20;
        long secondsLeft = 0;

        if (targetTicks > 0) {
            percent = (int) Math.min(100, Math.max(0, (elapsedTicks * 100) / targetTicks));
            ticksLeft = Math.max(0, targetTicks - elapsedTicks);
            secondsLeft = Math.max(0, (ticksLeft + 19) / 20);
        } else {
            percent = 100;
        }

        int percentLeft = Math.max(0, 100 - percent);

        // Progress bar calculation
        int barLength = (display != null && display.barLength() > 0) ? display.barLength() : 10;
        String barFilled = (display != null && display.barFilled() != null) ? display.barFilled() : "<green>■</green>";
        String barEmpty = (display != null && display.barEmpty() != null) ? display.barEmpty() : "<dark_gray>■</dark_gray>";

        int filledCount = (int) Math.round(((double) percent / 100.0) * barLength);
        filledCount = Math.min(barLength, Math.max(0, filledCount));
        int emptyCount = barLength - filledCount;

        StringBuilder progressBar = new StringBuilder(barLength * 16);
        for (int i = 0; i < filledCount; i++) {
            progressBar.append(barFilled);
        }
        for (int i = 0; i < emptyCount; i++) {
            progressBar.append(barEmpty);
        }

        // Phase metadata
        String phaseName = (phase.properties() != null && phase.properties().containsKey("name"))
                ? phase.properties().get("name")
                : capitalize(phase.id());
        int phaseIndex = instance.getCurrentPhaseIndex() + 1;
        int phaseTotal = (processDef != null && processDef.phases() != null) ? processDef.phases().size() : 1;

        // Recipe metadata
        String recipeName = "";
        if (instance.getContextVariables().containsKey("recipe_name")) {
            recipeName = String.valueOf(instance.getContextVariables().get("recipe_name"));
        } else if (context != null && context.getVariable("recipe.name").isPresent()) {
            recipeName = String.valueOf(context.getVariable("recipe.name").get());
        } else if (processDef != null) {
            recipeName = processDef.displayName();
        }

        // Temperature metadata
        String temperature = "20.0";
        if (context != null && context.getVariable("station.temperature").isPresent()) {
            Object tempObj = context.getVariable("station.temperature").get();
            if (tempObj instanceof Number num) {
                temperature = String.format(java.util.Locale.ROOT, "%.1f", num.doubleValue());
            } else {
                temperature = String.valueOf(tempObj);
            }
        }

        return template
                .replace("{progress_percent}", String.valueOf(percent))
                .replace("{progress_percent_left}", String.valueOf(percentLeft))
                .replace("{progress_seconds}", String.valueOf(secondsElapsed))
                .replace("{progress_seconds_left}", String.valueOf(secondsLeft))
                .replace("{progress_ticks}", String.valueOf(elapsedTicks))
                .replace("{progress_ticks_left}", String.valueOf(ticksLeft))
                .replace("{progress_bar}", progressBar.toString())
                .replace("{phase_name}", phaseName)
                .replace("{phase_index}", String.valueOf(phaseIndex))
                .replace("{phase_total}", String.valueOf(phaseTotal))
                .replace("{recipe_name}", recipeName)
                .replace("{temperature}", temperature);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).replace('_', ' ');
    }
}
