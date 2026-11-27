package dev.darkblade.croppy.platform.paper.command.selector;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ArgumentParseResult;

import java.util.List;

/**
 * Pluggable rule matching and resolving an {@code @}-prefixed target selector token
 * to a collection of domain targets.
 *
 * @param <C> command sender context type
 * @param <T> domain target type
 */
public interface SelectorRule<C, T> {

    /**
     * Checks whether this rule handles the given {@code @}-prefixed token.
     *
     * @param token raw selector token starting with {@code '@'}
     * @return true if this rule applies to the token
     */
    boolean matches(@NonNull String token);

    /**
     * Resolves matching targets for the given selector token.
     *
     * @param context command execution context
     * @param token   raw selector token starting with {@code '@'}
     * @return parse result containing matched targets or a descriptive failure
     */
    @NonNull ArgumentParseResult<List<T>> resolve(@NonNull CommandContext<C> context, @NonNull String token);

    /**
     * Returns selector completion candidates exposed by this rule.
     *
     * @param context   command execution context
     * @param remaining current partial input token
     * @return candidate selector tokens (each prefixed with {@code '@'})
     */
    @NonNull List<String> suggestions(@NonNull CommandContext<C> context, @NonNull String remaining);
}
