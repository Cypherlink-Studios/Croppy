package dev.darkblade.croppy.api.consumable;

import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.Map;

/**
 * Extensible handler executing an instant consumption effect when a custom item is consumed.
 */
public interface ConsumeEffectHandler extends Keyed {

    /**
     * Executes this effect for the consuming player.
     *
     * @param context    execution context containing player, item, quality, and expression scope
     * @param parameters declarative parameter map configured on the effect entry
     */
    void apply(ConsumeExecutionContext context, Map<String, Object> parameters);
}
