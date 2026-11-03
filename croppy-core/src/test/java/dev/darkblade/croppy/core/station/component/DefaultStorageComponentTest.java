package dev.darkblade.croppy.core.station.component;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StoredItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultStorageComponentTest {

    private final Key eggKey = Key.of("minecraft", "egg");
    private final Key milkKey = Key.of("minecraft", "milk_bucket");

    @Test
    @DisplayName("Should initialize with empty containers and valid capacities")
    void testInitialEmptyState() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        assertEquals(4, storage.getInputCapacity());
        assertEquals(2, storage.getOutputCapacity());
        assertTrue(storage.getInputs().isEmpty());
        assertTrue(storage.getOutputs().isEmpty());
    }

    @Test
    @DisplayName("Should stack identical input items into the same slot up to 64")
    void testAddInputStacking() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);

        assertTrue(storage.addInput(StoredItem.of(eggKey, 1)));
        assertTrue(storage.addInput(StoredItem.of(eggKey, 1)));
        assertTrue(storage.addInput(StoredItem.of(eggKey, 1)));
        assertTrue(storage.addInput(StoredItem.of(eggKey, 1)));

        List<StoredItem> inputs = storage.getInputs();
        assertEquals(1, inputs.size(), "All 4 individual eggs should be merged into a single slot");
        assertEquals(4, inputs.get(0).amount());
        assertEquals(eggKey, inputs.get(0).itemKey());
    }

    @Test
    @DisplayName("Should not stack items with different quality tiers into the same slot")
    void testAddInputDifferentQuality() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);

        assertTrue(storage.addInput(StoredItem.of(eggKey, 1, 0)));
        assertTrue(storage.addInput(StoredItem.of(eggKey, 1, 2)));

        List<StoredItem> inputs = storage.getInputs();
        assertEquals(2, inputs.size());
        assertEquals(1, inputs.get(0).amount());
        assertEquals(0, inputs.get(0).quality());
        assertEquals(1, inputs.get(1).amount());
        assertEquals(2, inputs.get(1).quality());
    }

    @Test
    @DisplayName("Should respect input capacity for distinct item types")
    void testAddInputCapacityLimit() {
        DefaultStorageComponent storage = new DefaultStorageComponent(2, 2);

        assertTrue(storage.addInput(StoredItem.of(eggKey, 10)));
        assertTrue(storage.addInput(StoredItem.of(milkKey, 1)));

        // Third distinct item cannot fit
        Key flourKey = Key.of("croppy", "flour");
        assertFalse(storage.addInput(StoredItem.of(flourKey, 1)));
        assertEquals(2, storage.getInputs().size());
    }

    @Test
    @DisplayName("Should cumulatively deduct across split slots when removing input")
    void testRemoveInputCumulative() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);

        // Intentionally create 2 slots of eggs with different qualities
        storage.addInput(StoredItem.of(eggKey, 2, 0));
        storage.addInput(StoredItem.of(eggKey, 3, 1));
        assertEquals(2, storage.getInputs().size());

        // Remove 4 eggs across both slots
        boolean removed = storage.removeInput(eggKey, 4);
        assertTrue(removed);

        List<StoredItem> remaining = storage.getInputs();
        assertEquals(1, remaining.size());
        assertEquals(1, remaining.get(0).amount());
        assertEquals(1, remaining.get(0).quality());
    }

    @Test
    @DisplayName("Should fail atomic removal without modifying inputs if insufficient total quantity")
    void testRemoveInputInsufficient() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        storage.addInput(StoredItem.of(eggKey, 3));

        boolean removed = storage.removeInput(eggKey, 5);
        assertFalse(removed);
        assertEquals(1, storage.getInputs().size());
        assertEquals(3, storage.getInputs().get(0).amount());
    }

    @Test
    @DisplayName("Should extract all input items atomically and clear inputs")
    void testExtractInputs() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        storage.addInput(StoredItem.of(eggKey, 4));
        storage.addInput(StoredItem.of(milkKey, 1));

        List<StoredItem> extracted = storage.extractInputs();
        assertEquals(2, extracted.size());
        assertTrue(storage.getInputs().isEmpty(), "Storage inputs must be empty after extractInputs()");
    }

    @Test
    @DisplayName("Should extract all output items atomically and clear outputs")
    void testExtractOutputs() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        storage.addOutput(StoredItem.of(Key.of("croppy", "fried_egg"), 2));

        List<StoredItem> extracted = storage.extractOutputs();
        assertEquals(1, extracted.size());
        assertEquals(2, extracted.get(0).amount());
        assertTrue(storage.getOutputs().isEmpty(), "Storage outputs must be empty after extractOutputs()");
    }

    @Test
    @DisplayName("Should clear all inputs and outputs on clear()")
    void testClear() {
        DefaultStorageComponent storage = new DefaultStorageComponent(4, 2);
        storage.addInput(StoredItem.of(eggKey, 2));
        storage.addOutput(StoredItem.of(milkKey, 1));

        storage.clear();
        assertTrue(storage.getInputs().isEmpty());
        assertTrue(storage.getOutputs().isEmpty());
    }
}
