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
}
