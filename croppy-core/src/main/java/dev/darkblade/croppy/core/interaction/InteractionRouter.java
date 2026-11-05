package dev.darkblade.croppy.core.interaction;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.Expression;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.interaction.InteractionDefinition;
import dev.darkblade.croppy.api.interaction.InteractionRegistry;
import dev.darkblade.croppy.api.interaction.InteractionTrigger;
import dev.darkblade.croppy.api.station.StationInstance;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Evaluates and routes player interaction events to matching {@link InteractionDefinition} pipelines
 * for both cooking stations and crop species.
 */
public final class InteractionRouter {

    private final InteractionRegistry interactionRegistry;

    public InteractionRouter(InteractionRegistry interactionRegistry) {
        this.interactionRegistry = Objects.requireNonNull(interactionRegistry, "interactionRegistry cannot be null");
    }

    /**
     * Resolves the highest-priority matching interaction definition for a given player interaction with a station.
     *
     * @param station      the target station instance
     * @param trigger      the interaction trigger (e.g. RIGHT_CLICK, SNEAK_RIGHT_CLICK)
     * @param handItemKey  custom item key in player hand, or null
     * @param handMaterial vanilla material identifier in hand (e.g. "minecraft:bowl"), or null
     * @param emptyHand    true if player hand is completely empty
     * @param context      evaluation context for condition expressions
     * @return matching interaction definition, if any
     */
    public Optional<InteractionDefinition> match(
            StationInstance station,
            InteractionTrigger trigger,
            Key handItemKey,
            String handMaterial,
            boolean emptyHand,
            EvaluationContext context
    ) {
        Objects.requireNonNull(station, "station cannot be null");
        List<InteractionDefinition> candidates = interactionRegistry.getInteractionsForStation(station.getStationKey());
        return matchCandidates(candidates, trigger, handItemKey, handMaterial, emptyHand, context);
    }

    /**
     * Resolves the highest-priority matching interaction definition for a given player interaction with a crop species.
     *
     * @param cropSpeciesKey the target crop species key
     * @param trigger        the interaction trigger (e.g. RIGHT_CLICK, LEFT_CLICK, SNEAK_LEFT_CLICK)
     * @param handItemKey    custom item key in player hand, or null
     * @param handMaterial   vanilla material identifier in hand (e.g. "minecraft:shears"), or null
     * @param emptyHand      true if player hand is completely empty
     * @param context        evaluation context for condition expressions
     * @return matching interaction definition, if any
     */
    public Optional<InteractionDefinition> matchCrop(
            Key cropSpeciesKey,
            InteractionTrigger trigger,
            Key handItemKey,
            String handMaterial,
            boolean emptyHand,
            EvaluationContext context
    ) {
        Objects.requireNonNull(cropSpeciesKey, "cropSpeciesKey cannot be null");
        List<InteractionDefinition> candidates = interactionRegistry.getInteractionsForCrop(cropSpeciesKey);
        return matchCandidates(candidates, trigger, handItemKey, handMaterial, emptyHand, context);
    }

    private Optional<InteractionDefinition> matchCandidates(
            List<InteractionDefinition> candidates,
            InteractionTrigger trigger,
            Key handItemKey,
            String handMaterial,
            boolean emptyHand,
            EvaluationContext context
    ) {
        Objects.requireNonNull(trigger, "trigger cannot be null");
        EvaluationContext evalCtx = (context == null) ? EvaluationContext.empty() : context;

        for (InteractionDefinition def : candidates) {
            // 1. Trigger check
            if (def.trigger() != trigger) {
                continue;
            }

            // 2. Empty hand requirement
            if (def.requireEmptyHand() && !emptyHand) {
                continue;
            }

            // 3. Match specific custom item key
            if (def.matchItem().isPresent()) {
                if (handItemKey == null || !def.matchItem().get().equals(handItemKey)) {
                    continue;
                }
            }

            // 4. Match vanilla material
            if (def.matchMaterial().isPresent()) {
                String expected = def.matchMaterial().get();
                if (handMaterial == null) {
                    continue;
                }
                if (expected.startsWith("tag:axes")) {
                    if (!handMaterial.endsWith("_axe")) continue;
                } else if (expected.startsWith("tag:hoes")) {
                    if (!handMaterial.endsWith("_hoe")) continue;
                } else if (expected.startsWith("tag:swords")) {
                    if (!handMaterial.endsWith("_sword")) continue;
                } else if (!expected.equalsIgnoreCase(handMaterial)) {
                    continue;
                }
            }

            // 5. Evaluate conditions via Pratt AST
            boolean conditionsMet = true;
            for (String condition : def.conditions()) {
                try {
                    Expression expr = ExpressionCompiler.compile(condition);
                    if (!expr.evaluate(evalCtx)) {
                        conditionsMet = false;
                        break;
                    }
                } catch (Exception ex) {
                    conditionsMet = false;
                    break;
                }
            }

            if (!conditionsMet) {
                continue;
            }

            // Highest priority candidate matched
            return Optional.of(def);
        }

        return Optional.empty();
    }
}
