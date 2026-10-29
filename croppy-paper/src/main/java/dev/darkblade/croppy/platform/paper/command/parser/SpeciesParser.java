package dev.darkblade.croppy.platform.paper.command.parser;

import dev.darkblade.croppy.api.identifier.Key;
import dev.darkblade.croppy.api.species.Species;
import dev.darkblade.croppy.api.species.SpeciesRegistry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Cloud 2.0 argument parser that parses a {@link Species} by its {@link Key}
 * and provides dynamic auto-completion suggestions from the {@link SpeciesRegistry}.
 *
 * @param <C> command sender context type
 */
public final class SpeciesParser<C> implements ArgumentParser<C, Species>, SuggestionProvider<C> {

    private final SpeciesRegistry speciesRegistry;

    public SpeciesParser(SpeciesRegistry speciesRegistry) {
        this.speciesRegistry = speciesRegistry;
    }

    public static <C> ParserDescriptor<C, Species> speciesParser(SpeciesRegistry speciesRegistry) {
        return ParserDescriptor.of(new SpeciesParser<>(speciesRegistry), Species.class);
    }

    @Override
    public @NonNull ArgumentParseResult<Species> parse(
            @NonNull CommandContext<C> commandContext,
            @NonNull CommandInput commandInput
    ) {
        String input = commandInput.readString();
        Key key;
        try {
            key = Key.parse(input);
        } catch (IllegalArgumentException e) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Invalid key format: '" + input + "'"));
        }

        Optional<Species> speciesOpt = speciesRegistry.get(key);
        if (speciesOpt.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Unknown species: '" + input + "'"));
        }

        return ArgumentParseResult.success(speciesOpt.get());
    }

    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return this;
    }

    @Override
    public @NonNull CompletableFuture<? extends Iterable<? extends Suggestion>> suggestionsFuture(
            @NonNull CommandContext<C> context,
            @NonNull CommandInput input
    ) {
        String remaining = input.peekString().toLowerCase();
        return CompletableFuture.completedFuture(
                speciesRegistry.stream()
                        .map(species -> species.getKey().asString())
                        .filter(keyStr -> keyStr.toLowerCase().contains(remaining) || remaining.isEmpty())
                        .map(Suggestion::suggestion)
                        .collect(Collectors.toList())
        );
    }
}
