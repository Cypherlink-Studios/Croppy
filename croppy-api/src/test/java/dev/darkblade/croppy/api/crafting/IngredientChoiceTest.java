package dev.darkblade.croppy.api.crafting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredientChoiceTest {

    @Test
    @DisplayName("Valid item ingredient creates successfully")
    void validItemIngredient() {
        IngredientChoice choice = new IngredientChoice("minecraft:iron_ingot", 2);
        assertEquals("minecraft:iron_ingot", choice.identifier());
        assertEquals(2, choice.amount());
        assertFalse(choice.isTag());
        assertEquals("minecraft:iron_ingot", choice.normalizedIdentifier());
    }

    @Test
    @DisplayName("Single-argument constructor defaults amount to 1")
    void defaultAmount() {
        IngredientChoice choice = new IngredientChoice("croppy:wooden_can");
        assertEquals("croppy:wooden_can", choice.identifier());
        assertEquals(1, choice.amount());
        assertFalse(choice.isTag());
    }

    @Test
    @DisplayName("Tag identifier detects tag flag and strips prefix in normalizedIdentifier")
    void tagIdentifier() {
        IngredientChoice choice = new IngredientChoice("#minecraft:planks");
        assertTrue(choice.isTag());
        assertEquals("#minecraft:planks", choice.identifier());
        assertEquals("minecraft:planks", choice.normalizedIdentifier());
    }

    @Test
    @DisplayName("Null or blank identifier throws exception")
    void invalidIdentifierThrows() {
        assertThrows(NullPointerException.class, () -> new IngredientChoice(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new IngredientChoice("   ", 1));
    }

    @Test
    @DisplayName("Non-positive amount throws exception")
    void nonPositiveAmountThrows() {
        assertThrows(IllegalArgumentException.class, () -> new IngredientChoice("minecraft:iron_ingot", 0));
        assertThrows(IllegalArgumentException.class, () -> new IngredientChoice("minecraft:iron_ingot", -1));
    }

    @Test
    @DisplayName("CraftingType enum contains SHAPED and SHAPELESS")
    void craftingTypeEnum() {
        assertEquals(2, CraftingType.values().length);
        assertNotNull(CraftingType.valueOf("SHAPED"));
        assertNotNull(CraftingType.valueOf("SHAPELESS"));
    }
}
