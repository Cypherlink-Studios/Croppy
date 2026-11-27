package dev.darkblade.croppy.platform.paper.command.selector;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ArgumentParseResult;

/**
 * Resolves a non-selector (single-target) command input token into a domain target.
 *
 * @param <C> command sender context type
 * @param <T> domain target type
 */
@FunctionalInterface
public interface SingleTargetResolver<C, T> {

    /**
     * Parses a single target identifier into a domain target.
     *
     * @param context command execution context
     * @param token   non-{@code '@'} raw input token
     * @return parse result with resolved target or descriptive failure
     */
    @NonNull ArgumentParseResult<T> resolve(@NonNull CommandContext<C> context, @NonNull String token);
}
