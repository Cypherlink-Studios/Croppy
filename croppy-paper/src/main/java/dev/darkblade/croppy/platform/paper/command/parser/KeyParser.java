package dev.darkblade.croppy.platform.paper.command.parser;

import dev.darkblade.croppy.api.identifier.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;

/**
 * Cloud 2.0 argument parser that parses a {@link Key} from command input.
 * Supports namespaced keys (e.g. 'croppy:wooden_can') and unqualified keys (e.g. 'wooden_can'),
 * defaulting unqualified keys to the 'croppy' namespace.
 *
 * @param <C> command sender context type
 */
public final class KeyParser<C> implements ArgumentParser<C, Key> {

    public static <C> ParserDescriptor<C, Key> keyParser() {
        return ParserDescriptor.of(new KeyParser<>(), Key.class);
    }

    @Override
    public @NonNull ArgumentParseResult<Key> parse(
            @NonNull CommandContext<C> commandContext,
            @NonNull CommandInput commandInput
    ) {
        String input = commandInput.readString();
        try {
            return ArgumentParseResult.success(Key.parse(input));
        } catch (IllegalArgumentException e) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Invalid key format: '" + input + "'"));
        }
    }
}
