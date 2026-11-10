package dev.darkblade.croppy.platform.paper.crafting;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemType;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Lightweight test implementation of Paper's {@link RegistryAccess} enabling
 * Bukkit recipe, block type, and material inspections in offline unit test runners.
 */
public class TestRegistryAccess implements RegistryAccess {

    @Override
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return createMockRegistry(type);
    }

    @Override
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> key) {
        Class<?> targetType = BlockType.class;
        if (key != null && key.toString().toLowerCase().contains("item")) {
            targetType = ItemType.class;
        }
        return createMockRegistry(targetType);
    }

    @SuppressWarnings("unchecked")
    private <T extends Keyed> Registry<T> createMockRegistry(Class<?> targetType) {
        Class<?> elementInterface = (targetType != null) ? targetType : BlockType.class;

        return (Registry<T>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Registry.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("get".equals(name) && args != null && args.length == 1 && args[0] instanceof NamespacedKey key) {
                        return Proxy.newProxyInstance(
                                getClass().getClassLoader(),
                                new Class<?>[]{elementInterface},
                                (p, m, a) -> {
                                    if ("isAir".equals(m.getName())) {
                                        return key.getKey().toLowerCase().contains("air");
                                    }
                                    if ("getKey".equals(m.getName())) {
                                        return key;
                                    }
                                    return null;
                                }
                        );
                    }
                    if ("size".equals(name)) {
                        return 0;
                    }
                    if ("stream".equals(name)) {
                        return Stream.empty();
                    }
                    if ("keyStream".equals(name)) {
                        return Stream.empty();
                    }
                    if ("iterator".equals(name)) {
                        return Collections.emptyIterator();
                    }
                    if ("hasTag".equals(name)) {
                        return false;
                    }
                    if ("getTags".equals(name)) {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );
    }
}
