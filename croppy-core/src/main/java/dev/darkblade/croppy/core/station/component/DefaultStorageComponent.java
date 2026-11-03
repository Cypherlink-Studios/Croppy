package dev.darkblade.croppy.core.station.component;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StoredItem;
import dev.darkblade.croppy.api.station.component.StorageComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe default implementation of {@link StorageComponent}.
 */
public final class DefaultStorageComponent implements StorageComponent {

    private static final int MAX_STACK_SIZE = 64;

    private final int inputCapacity;
    private final int outputCapacity;
    private final List<StoredItem> inputs = new ArrayList<>();
    private final List<StoredItem> outputs = new ArrayList<>();

    public DefaultStorageComponent(int inputCapacity, int outputCapacity) {
        this.inputCapacity = Math.max(1, inputCapacity);
        this.outputCapacity = Math.max(1, outputCapacity);
    }

    @Override
    public synchronized List<StoredItem> getInputs() {
        return List.copyOf(inputs);
    }

    @Override
    public synchronized List<StoredItem> getOutputs() {
        return List.copyOf(outputs);
    }

    @Override
    public synchronized boolean addInput(StoredItem item) {
        Objects.requireNonNull(item, "item cannot be null");
        if (item.amount() <= 0) {
            return false;
        }

        // 1. Try merging into existing matching stack
        for (int i = 0; i < inputs.size(); i++) {
            StoredItem existing = inputs.get(i);
            if (existing.itemKey().equals(item.itemKey()) && existing.quality() == item.quality()) {
                int space = MAX_STACK_SIZE - existing.amount();
                if (space >= item.amount()) {
                    inputs.set(i, existing.withAmount(existing.amount() + item.amount()));
                    return true;
                } else if (space > 0 && inputs.size() < inputCapacity) {
                    inputs.set(i, existing.withAmount(MAX_STACK_SIZE));
                    inputs.add(item.withAmount(item.amount() - space));
                    return true;
                }
            }
        }

        // 2. Add as a new slot if capacity allows
        if (inputs.size() >= inputCapacity) {
            return false;
        }
        inputs.add(item);
        return true;
    }

    @Override
    public synchronized boolean removeInput(Key itemKey, int amount) {
        Objects.requireNonNull(itemKey, "itemKey cannot be null");
        if (amount <= 0) {
            return true;
        }

        int totalAvailable = 0;
        for (StoredItem stored : inputs) {
            if (stored.itemKey().equals(itemKey)) {
                totalAvailable += stored.amount();
            }
        }
        if (totalAvailable < amount) {
            return false;
        }

        int remaining = amount;
        for (int i = 0; i < inputs.size() && remaining > 0; i++) {
            StoredItem stored = inputs.get(i);
            if (stored.itemKey().equals(itemKey)) {
                if (stored.amount() <= remaining) {
                    remaining -= stored.amount();
                    inputs.remove(i);
                    i--;
                } else {
                    inputs.set(i, stored.withAmount(stored.amount() - remaining));
                    remaining = 0;
                }
            }
        }
        return true;
    }

    @Override
    public synchronized boolean addOutput(StoredItem item) {
        Objects.requireNonNull(item, "item cannot be null");
        if (item.amount() <= 0) {
            return false;
        }

        for (int i = 0; i < outputs.size(); i++) {
            StoredItem existing = outputs.get(i);
            if (existing.itemKey().equals(item.itemKey()) && existing.quality() == item.quality()) {
                int space = MAX_STACK_SIZE - existing.amount();
                if (space >= item.amount()) {
                    outputs.set(i, existing.withAmount(existing.amount() + item.amount()));
                    return true;
                } else if (space > 0 && outputs.size() < outputCapacity) {
                    outputs.set(i, existing.withAmount(MAX_STACK_SIZE));
                    outputs.add(item.withAmount(item.amount() - space));
                    return true;
                }
            }
        }

        if (outputs.size() >= outputCapacity) {
            return false;
        }
        outputs.add(item);
        return true;
    }

    @Override
    public synchronized List<StoredItem> extractInputs() {
        List<StoredItem> extracted = new ArrayList<>(inputs);
        inputs.clear();
        return Collections.unmodifiableList(extracted);
    }

    @Override
    public synchronized List<StoredItem> extractOutputs() {
        List<StoredItem> extracted = new ArrayList<>(outputs);
        outputs.clear();
        return Collections.unmodifiableList(extracted);
    }

    @Override
    public synchronized void clear() {
        inputs.clear();
        outputs.clear();
    }

    @Override
    public int getInputCapacity() {
        return inputCapacity;
    }

    @Override
    public int getOutputCapacity() {
        return outputCapacity;
    }
}
