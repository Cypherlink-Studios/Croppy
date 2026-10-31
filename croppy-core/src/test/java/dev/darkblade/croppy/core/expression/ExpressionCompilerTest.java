package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionCompilerTest {

    @BeforeEach
    void setUp() {
        ExpressionCompiler.clearCache();
    }

    @Test
    void testBasicComparisons() {
        EvaluationContext context = EvaluationContext.of(Map.of("temp", 100.0, "level", 5));

        assertTrue(ExpressionCompiler.compile("temp >= 100").evaluate(context));
        assertTrue(ExpressionCompiler.compile("temp > 99.9").evaluate(context));
        assertFalse(ExpressionCompiler.compile("temp < 50").evaluate(context));
        assertTrue(ExpressionCompiler.compile("level == 5").evaluate(context));
        assertTrue(ExpressionCompiler.compile("level != 10").evaluate(context));
    }

    @Test
    void testLogicalCombinators() {
        EvaluationContext context = EvaluationContext.of(Map.of(
                "temp", 100.0,
                "fuel", 10,
                "is_creative", false
        ));

        assertTrue(ExpressionCompiler.compile("temp >= 100 && fuel > 0").evaluate(context));
        assertFalse(ExpressionCompiler.compile("temp >= 100 && fuel > 50").evaluate(context));
        assertTrue(ExpressionCompiler.compile("fuel > 50 || temp == 100").evaluate(context));
        assertTrue(ExpressionCompiler.compile("!is_creative").evaluate(context));
    }

    @Test
    void testArithmeticAndPrecedence() {
        EvaluationContext context = EvaluationContext.empty();

        // 2 + 3 * 4 = 14 (not 20)
        assertTrue(ExpressionCompiler.compile("2 + 3 * 4 == 14").evaluate(context));

        // Grouped (2 + 3) * 4 = 20
        assertTrue(ExpressionCompiler.compile("(2 + 3) * 4 == 20").evaluate(context));

        // Division and subtraction
        assertTrue(ExpressionCompiler.compile("100 / 2 - 10 == 40").evaluate(context));
    }

    @Test
    void testStringAndCollectionOperators() {
        EvaluationContext context = EvaluationContext.of(Map.of(
                "item", "croppy:tomato",
                "ingredients", List.of("croppy:tomato", "croppy:potato"),
                "tag", "sweet_flavor"
        ));

        assertTrue(ExpressionCompiler.compile("item == 'croppy:tomato'").evaluate(context));
        assertTrue(ExpressionCompiler.compile("item in ingredients").evaluate(context));
        assertTrue(ExpressionCompiler.compile("tag contains 'sweet'").evaluate(context));
    }

    @Test
    void testDynamicVariableResolvers() {
        EvaluationContext context = EvaluationContext.empty().withResolver((name, ctx) -> {
            if ("%vault_eco_balance%".equals(name)) {
                return java.util.Optional.of(250.75);
            }
            return java.util.Optional.empty();
        });

        assertTrue(ExpressionCompiler.compile("%vault_eco_balance% >= 200").evaluate(context));
        assertFalse(ExpressionCompiler.compile("%vault_eco_balance% < 100").evaluate(context));
    }

    @Test
    void testCacheReusesCompiledNode() {
        ExpressionNode first = ExpressionCompiler.compile("temp > 50");
        ExpressionNode second = ExpressionCompiler.compile("temp > 50");

        assertSame(first, second, "Compiled node must be cached and reused");
    }

    @Test
    void testInvalidSyntaxThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ExpressionCompiler.compile("temp >="));
        assertThrows(IllegalArgumentException.class, () -> ExpressionCompiler.compile("(temp > 50"));
        assertThrows(IllegalArgumentException.class, () -> ExpressionCompiler.compile("temp & 50"));
    }
}
