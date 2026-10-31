package dev.darkblade.croppy.platform.paper.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderApiVariableResolverTest {

    @Test
    void testNonPlaceholderIgnored() {
        PlaceholderApiVariableResolver resolver = new PlaceholderApiVariableResolver();
        EvaluationContext context = EvaluationContext.empty();

        assertEquals(Optional.empty(), resolver.resolve("simple_var", context));
        assertEquals(Optional.empty(), resolver.resolve("%", context));
        assertEquals(Optional.empty(), resolver.resolve("%%", context));
    }

    @Test
    void testPlaceholderFallbackWhenPapiAbsent() {
        PlaceholderApiVariableResolver resolver = new PlaceholderApiVariableResolver();
        EvaluationContext context = EvaluationContext.of(Map.of("player", "not_a_player_entity"));

        // In test environment without Bukkit/PAPI, it must return Optional.empty() without crashing
        assertEquals(Optional.empty(), resolver.resolve("%vault_eco_balance%", context));
    }
}
