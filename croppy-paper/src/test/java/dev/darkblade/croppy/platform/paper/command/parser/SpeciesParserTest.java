package dev.darkblade.croppy.platform.paper.command.parser;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.CropDisplay;
import dev.darkblade.croppy.api.species.CropRequirements;
import dev.darkblade.croppy.api.species.HarvestDefinition;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.StageDefinition;
import dev.darkblade.croppy.core.species.DefaultSpeciesRegistry;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.suggestion.Suggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class SpeciesParserTest {

    private DefaultSpeciesRegistry registry;
    private SpeciesParser<Source> parser;

    @BeforeEach
    void setUp() {
        registry = new DefaultSpeciesRegistry();
        Species wheat = new Species() {
            private final Key key = Key.croppy("wheat");
            private final CropDisplay display = new CropDisplay("vanilla", "minecraft:wheat", "minecraft:wheat_seeds");
            @Override public Key getKey() { return key; }
            @Override public String getName() { return "Wheat"; }
            @Override public CropDisplay getDisplay() { return display; }
            @Override public List<StageDefinition> getStages() { return List.of(); }
            @Override public StageDefinition getStage(int index) { return null; }
            @Override public CropRequirements getRequirements() { return null; }
            @Override public HarvestDefinition getHarvest() { return null; }
        };
        registry.register(wheat);

        parser = new SpeciesParser<>(registry);
    }

    @Test
    void testParseWithNamespace() {
        CommandInput input = CommandInput.of("croppy:wheat");
        ArgumentParseResult<Species> result = parser.parse(null, input);

        assertTrue(result.parsedValue().isPresent());
        assertEquals("croppy:wheat", result.parsedValue().get().getKey().asString());
    }

    @Test
    void testParseWithoutNamespaceDefaultsToCroppy() {
        CommandInput input = CommandInput.of("wheat");
        ArgumentParseResult<Species> result = parser.parse(null, input);

        assertTrue(result.parsedValue().isPresent());
        assertEquals("croppy:wheat", result.parsedValue().get().getKey().asString());
    }

    @Test
    void testParseUnknownSpeciesFails() {
        CommandInput input = CommandInput.of("croppy:corn");
        ArgumentParseResult<Species> result = parser.parse(null, input);

        assertTrue(result.failure().isPresent());
        assertTrue(result.failure().get().getMessage().contains("Unknown species"));
    }

    @Test
    void testSuggestions() throws Exception {
        CommandInput input = CommandInput.of("croppy:wh");
        var suggestions = parser.suggestionsFuture(null, input).get();

        List<String> list = StreamSupport.stream(suggestions.spliterator(), false)
                .map(Suggestion::suggestion)
                .toList();

        assertTrue(list.contains("croppy:wheat"));
    }
}
