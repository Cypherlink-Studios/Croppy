package dev.darkblade.croppy.core.consumable;

import dev.darkblade.croppy.api.consumable.ConsumeEffectHandler;
import dev.darkblade.croppy.api.consumable.ConsumeEffectRegistry;
import dev.darkblade.croppy.api.consumable.ConsumeExecutionContext;
import dev.darkblade.croppy.api.item.ConsumableProperties;
import dev.darkblade.croppy.api.item.ConsumeEffectSpec;
import dev.darkblade.croppy.api.logging.CroppyLogger;
import dev.darkblade.croppy.api.logging.LogChannel;
import dev.darkblade.croppy.api.logging.NoOpCroppyLogger;
import dev.darkblade.croppy.core.expression.ExpressionCompiler;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Evaluates and dispatches declarative {@link ConsumeEffectSpec} entries on completed consumption.
 */
public class ConsumptionEvaluator {

    private final ConsumeEffectRegistry registry;
    private final CroppyLogger logger;
    private final Supplier<Random> randomSupplier;

    public ConsumptionEvaluator(ConsumeEffectRegistry registry, CroppyLogger logger) {
        this(registry, logger, ThreadLocalRandom::current);
    }

    public ConsumptionEvaluator(ConsumeEffectRegistry registry, CroppyLogger logger, Supplier<Random> randomSupplier) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.logger = logger != null ? logger : NoOpCroppyLogger.INSTANCE;
        this.randomSupplier = Objects.requireNonNull(randomSupplier, "randomSupplier cannot be null");
    }

    /**
     * Evaluates all effect specs in {@code properties} against {@code context} and invokes matching handlers.
     *
     * @return number of handlers actually executed
     */
    public int execute(ConsumableProperties properties, ConsumeExecutionContext context) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        int executed = 0;
        Random rng = randomSupplier.get();

        for (ConsumeEffectSpec spec : properties.effects()) {
            if (context.getQuality() < spec.minQuality()) {
                continue;
            }
            if (spec.chance() < 1.0 && rng.nextDouble() >= spec.chance()) {
                continue;
            }
            if (spec.getCondition().isPresent()) {
                boolean passed;
                try {
                    passed = ExpressionCompiler.compile(spec.getCondition().get())
                            .evaluate(context.getEvaluationContext());
                } catch (Exception e) {
                    logger.warn(LogChannel.CORE,
                            "Failed to evaluate consume effect condition '" + spec.getCondition().get()
                                    + "' on item " + context.getItemDefinition().key() + ": " + e.getMessage());
                    continue;
                }
                if (!passed) {
                    continue;
                }
            }

            Optional<ConsumeEffectHandler> handlerOpt = registry.get(spec.type());
            if (handlerOpt.isEmpty()) {
                logger.warn(LogChannel.CORE,
                        "Unregistered consume effect handler '" + spec.type()
                                + "' referenced by item " + context.getItemDefinition().key());
                continue;
            }

            try {
                handlerOpt.get().apply(context, spec.parameters());
                executed++;
            } catch (Exception e) {
                logger.error(LogChannel.CORE,
                        "Error executing consume effect '" + spec.type()
                                + "' on item " + context.getItemDefinition().key(), e);
            }
        }
        return executed;
    }
}
