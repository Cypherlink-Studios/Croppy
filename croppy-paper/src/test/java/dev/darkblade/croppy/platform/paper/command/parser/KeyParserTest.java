package dev.darkblade.croppy.platform.paper.command.parser;

import dev.darkblade.croppy.api.identifier.Key;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyParserTest {

    private KeyParser<Source> parser;

    @BeforeEach
    void setUp() {
        parser = new KeyParser<>();
    }

    @Test
    @DisplayName("Parse key with namespace")
    void testParseWithNamespace() {
        CommandInput input = CommandInput.of("croppy:rusty_can");
        ArgumentParseResult<Key> result = parser.parse(null, input);

        assertTrue(result.parsedValue().isPresent());
        assertEquals(Key.of("croppy", "rusty_can"), result.parsedValue().get());
    }

    @Test
    @DisplayName("Parse key without namespace defaults to croppy")
    void testParseWithoutNamespace() {
        CommandInput input = CommandInput.of("rusty_can");
        ArgumentParseResult<Key> result = parser.parse(null, input);

        assertTrue(result.parsedValue().isPresent());
        assertEquals(Key.of("croppy", "rusty_can"), result.parsedValue().get());
    }

    @Test
    @DisplayName("Parse key with custom namespace")
    void testParseCustomNamespace() {
        CommandInput input = CommandInput.of("custom:golden_hoe");
        ArgumentParseResult<Key> result = parser.parse(null, input);

        assertTrue(result.parsedValue().isPresent());
        assertEquals(Key.of("custom", "golden_hoe"), result.parsedValue().get());
    }

    @Test
    @DisplayName("Parse invalid key format returns failure")
    void testParseInvalidFormat() {
        CommandInput input = CommandInput.of("invalid$key!");
        ArgumentParseResult<Key> result = parser.parse(null, input);

        assertTrue(result.failure().isPresent());
    }
}
