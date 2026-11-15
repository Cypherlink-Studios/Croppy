package dev.darkblade.croppy.core.variable;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.variable.OperationArgs;
import dev.darkblade.croppy.api.variable.SimpleDataType;
import dev.darkblade.croppy.api.variable.VariableOperation;
import dev.darkblade.croppy.api.variable.VariableSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypedVariableSystemTest {

    private DefaultDataTypeRegistry registry;
    private DefaultTypedVariableStore store;

    @BeforeEach
    void setUp() {
        registry = new DefaultDataTypeRegistry();
        store = new DefaultTypedVariableStore();
    }

    @Test
    void registryContainsStandardTypes() {
        assertTrue(registry.contains(IntegerDataType.KEY));
        assertTrue(registry.contains(DecimalDataType.KEY));
        assertTrue(registry.contains(BooleanDataType.KEY));
        assertTrue(registry.contains(StringDataType.KEY));
        assertTrue(registry.contains(MapCompoundDataType.KEY));
        assertEquals(5, registry.size());
    }

    @Test
    void registrySupportsCustomAddonDataTypes() {
        Key customKey = Key.of("alchemy", "mana");
        SimpleDataType<Integer> manaType = new SimpleDataType<>() {
            @Override
            public Key getKey() { return customKey; }
            @Override
            public Class<Integer> getValueClass() { return Integer.class; }
            @Override
            public Integer getDefaultValue() { return 0; }
            @Override
            public Integer parse(Object rawInput) { return Integer.parseInt(rawInput.toString()); }
            @Override
            public void writeBinary(DataOutput out, Integer value) throws IOException { out.writeInt(value); }
            @Override
            public Integer readBinary(DataInput in) throws IOException { return in.readInt(); }
            @Override
            public Collection<VariableOperation<Integer>> getSupportedOperations() {
                return List.of(StandardOperations.set(this));
            }
        };

        registry.register(manaType);
        assertTrue(registry.contains(customKey));
        assertTrue(registry.getTyped(customKey, Integer.class).isPresent());
    }

    @Test
    void numericOperationsMutateSafelyAndMarkDirty() {
        var varInt = store.define("cleanliness", IntegerDataType.INSTANCE, 50L);
        assertFalse(varInt.isDirty());

        Object afterAdd = store.applyOperation("cleanliness", "ADD", OperationArgs.of("amount", 25));
        assertEquals(75L, afterAdd);
        assertTrue(store.isDirty());

        Object afterSub = store.applyOperation("cleanliness", "SUBTRACT", OperationArgs.of("amount", 100));
        assertEquals(-25L, afterSub);

        Object afterClamp = store.applyOperation("cleanliness", "CLAMP", OperationArgs.of(Map.of("min", 0, "max", 100)));
        assertEquals(0L, afterClamp);

        store.define("heat_factor", DecimalDataType.INSTANCE, 1.5);
        Object afterMul = store.applyOperation("heat_factor", "MULTIPLY", OperationArgs.of("amount", 2.0));
        assertEquals(3.0, (Double) afterMul, 1e-6);
    }

    @Test
    void booleanAndStringOperationsWorkCorrectly() {
        store.define("is_covered", BooleanDataType.INSTANCE, false);
        assertEquals(true, store.applyOperation("is_covered", "TOGGLE", OperationArgs.EMPTY));
        assertEquals(false, store.applyOperation("is_covered", "TOGGLE", OperationArgs.EMPTY));

        store.define("label", StringDataType.INSTANCE, "Brew");
        assertEquals("Brew_v2", store.applyOperation("label", "APPEND", OperationArgs.of("value", "_v2")));
    }

    @Test
    void unsupportedOperationsThrowFastWithoutMutating() {
        store.define("is_covered", BooleanDataType.INSTANCE, true);
        assertThrows(UnsupportedOperationException.class, () ->
                store.applyOperation("is_covered", "ADD", OperationArgs.of("amount", 5))
        );
        assertEquals(true, store.getTypedVariable("is_covered", Boolean.class).orElseThrow().getValue());

        store.define("note", StringDataType.INSTANCE, "hello");
        assertThrows(UnsupportedOperationException.class, () ->
                store.applyOperation("note", "TOGGLE", OperationArgs.EMPTY)
        );
        assertEquals("hello", store.getTypedVariable("note", String.class).orElseThrow().getValue());
    }

    @Test
    void compoundTypeSupportsNestedExportAndMutations() {
        Map<String, Object> initialTank = MapCompoundDataType.INSTANCE.parse(Map.of(
                "fluid", "croppy:tea",
                "amount", 250,
                "tags", List.of("hot", "herbal")
        ));
        store.define("tank", MapCompoundDataType.INSTANCE, initialTank);

        EvaluationContext ctx = EvaluationContext.empty().withProvider(store);
        assertEquals("croppy:tea", ctx.getVariable("var.tank.fluid").orElse(null));
        assertEquals(250L, ctx.getVariable("var.tank.amount").orElse(null));
        assertEquals("hot", ctx.getVariable("var.tank.tags.0").orElse(null));
        assertEquals(2, ctx.getVariable("var.tank.tags.size").orElse(null));

        // Put new property
        store.applyOperation("tank", "PUT", OperationArgs.of(Map.of("key", "amount", "value", 500)));
        ctx = EvaluationContext.empty().withProvider(store);
        assertEquals(500L, ctx.getVariable("var.tank.amount").orElse(null));

        // Remove property
        store.applyOperation("tank", "REMOVE", OperationArgs.of("key", "fluid"));
        ctx = EvaluationContext.empty().withProvider(store);
        assertTrue(ctx.getVariable("var.tank.fluid").isEmpty());

        // Clear compound
        store.applyOperation("tank", "CLEAR", OperationArgs.EMPTY);
        assertTrue(store.getTypedVariable("tank", Map.class).orElseThrow().getValue().isEmpty());
    }

    @Test
    void initializeFromSpecsRespectsExistingVariablesAndParsesValues() {
        List<VariableSpec> specs = List.of(
                new VariableSpec("cleanliness", IntegerDataType.KEY, 100),
                new VariableSpec("covered", BooleanDataType.KEY, "true"),
                new VariableSpec("ratio", DecimalDataType.KEY, "0.75")
        );
        store.define("cleanliness", IntegerDataType.INSTANCE, 40L); // Pre-existing

        store.initializeFromSpecs(specs, registry);

        assertEquals(40L, store.getTypedVariable("cleanliness", Long.class).orElseThrow().getValue());
        assertEquals(true, store.getTypedVariable("covered", Boolean.class).orElseThrow().getValue());
        assertEquals(0.75, store.getTypedVariable("ratio", Double.class).orElseThrow().getValue());

        EvaluationContext ctx = EvaluationContext.empty().withProvider(store);
        assertEquals("0.8", ctx.getVariable("var.ratio_formatted").orElse(null));
    }
}
