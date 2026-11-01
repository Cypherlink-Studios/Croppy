package dev.darkblade.croppy.core.i18n;

import dev.darkblade.croppy.api.i18n.LocaleRef;
import dev.darkblade.croppy.api.i18n.MessageKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMessageCatalogTest {

    private DefaultMessageCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new DefaultMessageCatalog(LocaleRef.EN_US, LocaleRef.EN_US);

        Map<String, String> enUS = Map.of(
                "greeting", "Hello!",
                "only_en", "English only message",
                "shared", "Shared English"
        );

        Map<String, String> es = Map.of(
                "greeting", "¡Hola!",
                "base_only", "Solo en español base"
        );

        Map<String, String> esES = Map.of(
                "greeting", "¡Hola desde España!",
                "shared", "Compartido Español"
        );

        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, enUS,
                LocaleRef.of("es"), es,
                LocaleRef.ES_ES, esES
        ));
    }

    @Test
    void testDirectMatch() {
        Optional<String> msg = catalog.getTemplate(LocaleRef.ES_ES, MessageKey.of("greeting"));
        assertTrue(msg.isPresent());
        assertEquals("¡Hola desde España!", msg.get());
    }

    @Test
    void testBaseLanguageFallback() {
        // es-AR should fall back to base 'es' for 'greeting' and 'base_only'
        LocaleRef esAR = LocaleRef.of("es", "AR");

        Optional<String> baseMsg = catalog.getTemplate(esAR, MessageKey.of("base_only"));
        assertTrue(baseMsg.isPresent());
        assertEquals("Solo en español base", baseMsg.get());

        Optional<String> greeting = catalog.getTemplate(esAR, MessageKey.of("greeting"));
        assertTrue(greeting.isPresent());
        assertEquals("¡Hola!", greeting.get());
    }

    @Test
    void testSourceLocaleFallback() {
        // Key 'only_en' doesn't exist in es-ES or es, should fall back to en-US
        Optional<String> msg = catalog.getTemplate(LocaleRef.ES_ES, MessageKey.of("only_en"));
        assertTrue(msg.isPresent());
        assertEquals("English only message", msg.get());
    }

    @Test
    void testCompletelyMissingKey() {
        Optional<String> msg = catalog.getTemplate(LocaleRef.ES_ES, MessageKey.of("non_existent"));
        assertTrue(msg.isEmpty());
    }

    @Test
    void testMissingKeyDeduplication() {
        assertTrue(catalog.markMissingKeyReported("missing.key.1"));
        assertFalse(catalog.markMissingKeyReported("missing.key.1"));
        assertTrue(catalog.markMissingKeyReported("missing.key.2"));
    }

    @Test
    void testAtomicReplacement() {
        catalog.setCatalogs(Map.of(
                LocaleRef.EN_US, Map.of("new_key", "New Value")
        ));

        assertTrue(catalog.getTemplate(LocaleRef.EN_US, MessageKey.of("new_key")).isPresent());
        assertTrue(catalog.getTemplate(LocaleRef.EN_US, MessageKey.of("greeting")).isEmpty());
    }
}
