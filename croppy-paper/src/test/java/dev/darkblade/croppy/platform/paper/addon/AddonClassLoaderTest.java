package dev.darkblade.croppy.platform.paper.addon;

import dev.darkblade.croppy.api.addon.AddonDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AddonClassLoaderTest {

    @TempDir
    Path tempDir;

    // Sample class to package into JAR A
    public static class SampleAddonClassA {
        public String hello() {
            return "from addon A";
        }
    }

    private Path createJarWithClass(String jarName, Class<?> clazz) throws IOException {
        Path jarPath = tempDir.resolve(jarName);
        String resourcePath = clazz.getName().replace('.', '/') + ".class";
        try (InputStream in = clazz.getClassLoader().getResourceAsStream(resourcePath);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            assertNotNull(in, "Class resource not found: " + resourcePath);
            jos.putNextEntry(new JarEntry(resourcePath));
            in.transferTo(jos);
            jos.closeEntry();
        }
        return jarPath;
    }

    private AddonDescription createDesc(String id, List<String> deps) {
        return new AddonDescription(
                id, id, "1.0", id, Optional.of("dummy.Main"), List.of(), "", deps, List.of()
        );
    }

    @Test
    @DisplayName("Horizontal delegation resolves dependency classes across class loaders")
    void testHorizontalClassDelegation() throws Exception {
        Path jarA = createJarWithClass("addon-a.jar", SampleAddonClassA.class);
        Path jarB = tempDir.resolve("addon-b.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarB.toFile()))) {
            jos.putNextEntry(new JarEntry("dummy.txt"));
            jos.write("content".getBytes());
            jos.closeEntry();
        }

        // Parent classloader without the test class
        ClassLoader isolatedParent = ClassLoader.getPlatformClassLoader();

        AddonClassLoader loaderA = new AddonClassLoader(
                new URL[]{jarA.toUri().toURL()},
                isolatedParent,
                createDesc("addon-a", List.of())
        );

        AddonClassLoader loaderB = new AddonClassLoader(
                new URL[]{jarB.toUri().toURL()},
                isolatedParent,
                createDesc("addon-b", List.of("addon-a"))
        );

        // B depends on A
        loaderB.addDependencyLoader(loaderA);

        // Loader B can find and load class from Loader A via horizontal delegation
        Class<?> resolvedClass = loaderB.loadClass(SampleAddonClassA.class.getName());
        assertNotNull(resolvedClass);
        assertEquals(loaderA, resolvedClass.getClassLoader());

        // Independent Loader C cannot find class from Loader A
        AddonClassLoader loaderC = new AddonClassLoader(
                new URL[]{jarB.toUri().toURL()},
                isolatedParent,
                createDesc("addon-c", List.of())
        );

        assertThrows(ClassNotFoundException.class, () ->
                loaderC.loadClass(SampleAddonClassA.class.getName()));

        loaderA.close();
        loaderB.close();
        loaderC.close();
    }

    @Test
    @DisplayName("AddonClassLoader isolates host config.yml and prioritizes local resources")
    void testResourceIsolation() throws Exception {
        // Parent classloader that contains host config.yml
        ClassLoader parentWithConfig = getClass().getClassLoader();
        assertNotNull(parentWithConfig.getResource("config.yml"), "CroppyPlugin test resources should contain config.yml");

        // JAR without config.yml
        Path emptyJar = tempDir.resolve("empty-addon.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(emptyJar.toFile()))) {
            jos.putNextEntry(new JarEntry("dummy.txt"));
            jos.write("content".getBytes());
            jos.closeEntry();
        }

        AddonClassLoader loaderWithoutConfig = new AddonClassLoader(
                new URL[]{emptyJar.toUri().toURL()},
                parentWithConfig,
                createDesc("empty-addon", List.of())
        );

        // Must NOT leak parent's config.yml
        assertNull(loaderWithoutConfig.getResource("config.yml"));
        assertNull(loaderWithoutConfig.getResourceAsStream("config.yml"));

        // JAR with its own config.yml
        Path customJar = tempDir.resolve("custom-addon.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(customJar.toFile()))) {
            jos.putNextEntry(new JarEntry("config.yml"));
            jos.write("custom_addon_property: true\n".getBytes());
            jos.closeEntry();
        }

        AddonClassLoader loaderWithConfig = new AddonClassLoader(
                new URL[]{customJar.toUri().toURL()},
                parentWithConfig,
                createDesc("custom-addon", List.of())
        );

        // Must resolve its own local config.yml
        URL localUrl = loaderWithConfig.getResource("config.yml");
        assertNotNull(localUrl);
        String content = new String(loaderWithConfig.getResourceAsStream("config.yml").readAllBytes());
        assertTrue(content.contains("custom_addon_property: true"));

        loaderWithoutConfig.close();
        loaderWithConfig.close();
    }
}
