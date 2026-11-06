package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Isolated class loader for a Croppy Java addon supporting horizontal delegation
 * to declared addon dependencies before falling back to the parent host class loader.
 */
public class AddonClassLoader extends URLClassLoader {

    private final AddonDescription description;
    private final Set<AddonClassLoader> dependencyLoaders = new CopyOnWriteArraySet<>();

    public AddonClassLoader(URL[] urls, ClassLoader parent, AddonDescription description) {
        super(urls, parent);
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    public AddonDescription getDescription() {
        return description;
    }

    public void addDependencyLoader(AddonClassLoader loader) {
        if (loader != null && loader != this) {
            dependencyLoaders.add(loader);
        }
    }

    public void removeDependencyLoader(AddonClassLoader loader) {
        if (loader != null) {
            dependencyLoaders.remove(loader);
        }
    }

    public Set<AddonClassLoader> getDependencyLoaders() {
        return Set.copyOf(dependencyLoaders);
    }

    /**
     * Attempts to find a class strictly within this class loader's local URLs
     * without delegating to dependencies or parent.
     *
     * @param name binary name of the class
     * @return the resolved Class
     * @throws ClassNotFoundException if the class is not defined in this loader's URLs
     */
    public Class<?> findClassLocally(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }
            return super.findClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 1. Try locally first
        try {
            return findClassLocally(name);
        } catch (ClassNotFoundException ignored) {
        }

        // 2. Delegate horizontally across declared dependencies (without recursive re-delegation)
        for (AddonClassLoader depLoader : dependencyLoaders) {
            try {
                return depLoader.findClassLocally(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        // 3. Fallback to throwing ClassNotFoundException for parent handling
        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // Check if already loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                // If it's a core platform/runtime class, prioritize parent
                if (isPlatformClass(name)) {
                    c = getParent().loadClass(name);
                } else {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        c = getParent().loadClass(name);
                    }
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private boolean isPlatformClass(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("org.bukkit.")
                || name.startsWith("io.papermc.")
                || name.startsWith("net.kyori.")
                || name.startsWith("dev.darkblade.croppy.api.");
    }

    @Override
    public void close() throws IOException {
        dependencyLoaders.clear();
        super.close();
    }
}
