package dev.darkblade.croppy.platform.paper.crafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Lightweight test stub of Bukkit's {@link ItemStack} for offline unit testing
 * without Paper server runtime bootstrapping.
 */
public class MockTestItemStack extends ItemStack {

    private final Material material;
    private final int amount;

    public MockTestItemStack(Material material) {
        this(material, 1);
    }

    public MockTestItemStack(Material material, int amount) {
        this.material = (material != null) ? material : Material.AIR;
        this.amount = amount;
    }

    @Override
    public Material getType() {
        return material;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public boolean isEmpty() {
        return material == Material.AIR || amount <= 0;
    }

    @Override
    public boolean hasItemMeta() {
        return false;
    }

    @Override
    public ItemStack clone() {
        return new MockTestItemStack(material, amount);
    }

    @Override
    public boolean isSimilar(ItemStack stack) {
        return stack != null && stack.getType() == material;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemStack other) {
            return other.getType() == material && other.getAmount() == amount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return material.hashCode() * 31 + amount;
    }

    @Override
    public String toString() {
        return "MockItemStack{" + material + " x" + amount + "}";
    }
}
