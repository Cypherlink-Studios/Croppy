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
        if (inputs.size() >= inputCapacity) {
            return false;
        }
        inputs.add(item);
        return true;
    }

    @Override
    public synchronized boolean removeInput(Key itemKey, int amount) {
        Objects.requireNonNull(itemKey, "itemKey cannot be null");
        for (int i = 0; i < inputs.size(); i++) {
            StoredItem stored = inputs.get(i);
            if (stored.itemKey().equals(itemKey)) {
                if (stored.amount() > amount) {
                    inputs.set(i, stored.withAmount(stored.amount() - amount));
                    return true;
                } else if (stored.amount() == amount) {
                    inputs.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized boolean addOutput(StoredItem item) {
        Objects.requireNonNull(item, "item cannot be null");
        if (outputs.size() >= outputCapacity) {
            return false;
        }
        outputs.add(item);
        return true;
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
