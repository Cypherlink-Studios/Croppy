package dev.darkblade.croppy.api.station.component;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.station.StoredItem;

import java.util.List;

/**
 * Component managing input and output storage containers of a station.
 */
public interface StorageComponent extends StationComponent {

    /**
     * @return unmodifiable view of current input items
     */
    List<StoredItem> getInputs();

    /**
     * @return unmodifiable view of current output items
     */
    List<StoredItem> getOutputs();

    /**
     * Attempts to add an item to the inputs container.
     *
     * @param item the item to insert
     * @return true if inserted, false if capacity reached
     */
    boolean addInput(StoredItem item);

    /**
     * Removes a specified quantity of an input item.
     *
     * @param itemKey the item key to remove
     * @param amount  quantity to remove
     * @return true if successfully removed
     */
    boolean removeInput(Key itemKey, int amount);

    /**
     * Adds an item to the output container.
     *
     * @param item the item to deposit
     * @return true if deposited, false if output full
     */
    boolean addOutput(StoredItem item);

    /**
     * Extracts and clears all output items.
     *
     * @return extracted items
     */
    List<StoredItem> extractOutputs();

    /**
     * Clears all inputs and outputs.
     */
    void clear();

    /**
     * @return max input slot capacity
     */
    int getInputCapacity();

    /**
     * @return max output slot capacity
     */
    int getOutputCapacity();
}
