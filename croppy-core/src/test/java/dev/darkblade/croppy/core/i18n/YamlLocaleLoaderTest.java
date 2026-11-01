package dev.darkblade.croppy.core.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlLocaleLoaderTest {

    private final YamlLocaleLoader loader = new YamlLocaleLoader();

    @Test
    void testFlattenNestedYaml() {
        String yaml = """
                command:
                  item:
                    give:
                      success: "<green>Given <amount>x <item> to <player>.</green>"
                  reload:
                    success: "<green>Reloaded.</green>"
                general:
                  prefix: "<gold>[Croppy]</gold>"
                """;

        InputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        Map<String, String> map = loader.load(in, "test.yml");

        assertEquals("<green>Given <amount>x <item> to <player>.</green>", map.get("command.item.give.success"));
        assertEquals("<green>Reloaded.</green>", map.get("command.reload.success"));
        assertEquals("<gold>[Croppy]</gold>", map.get("general.prefix"));
    }

    @Test
    void testMultilineListJoinedWithNewline() {
        String yaml = """
                item:
                  wheat_seeds:
                    lore:
                      - "<gray>Plant on farmland</gray>"
                      - "<yellow>Grows in 4 stages</yellow>"
                """;

        InputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        Map<String, String> map = loader.load(in, "test.yml");

        String expected = "<gray>Plant on farmland</gray>\n<yellow>Grows in 4 stages</yellow>";
        assertEquals(expected, map.get("item.wheat_seeds.lore"));
    }

    @Test
    void testInvalidYamlThrows() {
        String invalid = "- just\n- a\n- list\n";
        InputStream in = new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> loader.load(in, "invalid.yml"));
    }

    @Test
    void testLoadDirectory(@TempDir Path tempDir) throws Exception {
        Path enFile = tempDir.resolve("en-US.yml");
        Path esFile = tempDir.resolve("es-ES.yml");

        Files.writeString(enFile, "hello: \"Hello\"\nworld: \"World\"\n");
        Files.writeString(esFile, "hello: \"Hola\"\nworld: \"Mundo\"\n");

        Map<LocaleRef, Map<String, String>> loaded = loader.loadDirectory(tempDir, null);

        assertEquals(2, loaded.size());
        assertTrue(loaded.containsKey(LocaleRef.EN_US));
        assertTrue(loaded.containsKey(LocaleRef.ES_ES));
        assertEquals("Hello", loaded.get(LocaleRef.EN_US).get("hello"));
        assertEquals("Hola", loaded.get(LocaleRef.ES_ES).get("hello"));
    }
}
