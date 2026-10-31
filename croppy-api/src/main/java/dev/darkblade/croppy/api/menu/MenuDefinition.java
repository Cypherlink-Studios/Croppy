package dev.darkblade.croppy.api.menu;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.identifier.Keyed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable definition of a configurable inventory menu layout.
 *
 * @param key        unique namespaced identifier
 * @param title      MiniMessage-formatted title supporting placeholder replacement
 * @param rows       number of 9-slot inventory rows (1 to 6)
 * @param pattern    list of sanitized 9-character pattern row strings
 * @param elements   mapping of pattern characters to slot definitions
 * @param properties arbitrary metadata properties
 */
public record MenuDefinition(
        Key key,
        String title,
        int rows,
        List<String> pattern,
        Map<Character, SlotDefinition> elements,
        Map<String, String> properties
) implements Keyed {

    public MenuDefinition {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Menu rows must be between 1 and 6, got " + rows);
        }
        Objects.requireNonNull(pattern, "pattern cannot be null");
        if (pattern.size() != rows) {
            throw new IllegalArgumentException("Pattern lines count (" + pattern.size() + ") must match rows count (" + rows + ")");
        }
        for (int r = 0; r < pattern.size(); r++) {
            String line = pattern.get(r);
            Objects.requireNonNull(line, "Pattern line " + r + " cannot be null");
            if (line.length() != 9) {
                throw new IllegalArgumentException("Pattern row " + r + " length must be exactly 9, got " + line.length() + ": '" + line + "'");
            }
        }
        pattern = List.copyOf(pattern);
        elements = (elements == null) ? Map.of() : Map.copyOf(elements);
        properties = (properties == null) ? Map.of() : Map.copyOf(properties);
    }

    @Override
    public Key getKey() {
        return key;
    }

    /**
     * Total inventory slot count (rows * 9).
     *
     * @return slot capacity
     */
    public int size() {
        return rows * 9;
    }

    /**
     * Retrieves the character symbol at the specified 0-based inventory slot index.
     *
     * @param slotIndex 0-based slot index
     * @return character symbol
     * @throws IndexOutOfBoundsException if slotIndex is negative or >= size()
     */
    public char getSymbolAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= size()) {
            throw new IndexOutOfBoundsException("Slot index out of bounds: " + slotIndex + " (size: " + size() + ")");
        }
        int row = slotIndex / 9;
        int col = slotIndex % 9;
        return pattern.get(row).charAt(col);
    }

    /**
     * Retrieves the {@link SlotDefinition} assigned to the specified slot index.
     *
     * @param slotIndex 0-based slot index
     * @return optional slot definition if mapped
     */
    public Optional<SlotDefinition> getSlotAt(int slotIndex) {
        char symbol = getSymbolAt(slotIndex);
        return Optional.ofNullable(elements.get(symbol));
    }

    /**
     * Returns an ordered list of 0-based slot indices whose definition matches the specified {@link SlotType}.
     *
     * @param type slot type to filter
     * @return ordered slot indices
     */
    public List<Integer> getSlotsForType(SlotType type) {
        Objects.requireNonNull(type, "type cannot be null");
        List<Integer> slots = new ArrayList<>();
        int total = size();
        for (int i = 0; i < total; i++) {
            char symbol = getSymbolAt(i);
            SlotDefinition def = elements.get(symbol);
            if (def != null && def.type() == type) {
                slots.add(i);
            }
        }
        return List.copyOf(slots);
    }
}
