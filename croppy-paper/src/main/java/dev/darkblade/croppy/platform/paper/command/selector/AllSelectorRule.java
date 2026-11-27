package dev.darkblade.croppy.platform.paper.command.selector;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ArgumentParseResult;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Reusable {@link SelectorRule} handling the {@code @all} wildcard selector.
 *
 * @param <C> command sender context type
 * @param <T> domain target type
 */
public final class AllSelectorRule<C, T> implements SelectorRule<C, T> {

    public static final String TOKEN = "@all";

    private final Supplier<Collection<T>> targetsSupplier;
    private final String emptyMessage;

    public AllSelectorRule(Supplier<Collection<T>> targetsSupplier, String emptyMessage) {
        this.targetsSupplier = Objects.requireNonNull(targetsSupplier, "targetsSupplier cannot be null");
        this.emptyMessage = Objects.requireNonNull(emptyMessage, "emptyMessage cannot be null");
    }

    @Override
    public boolean matches(@NonNull String token) {
        return TOKEN.equalsIgnoreCase(token);
    }

    @Override
    public @NonNull ArgumentParseResult<List<T>> resolve(@NonNull CommandContext<C> context, @NonNull String token) {
        Collection<T> targets = targetsSupplier.get();
        if (targets == null || targets.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException(emptyMessage));
        }
        return ArgumentParseResult.success(List.copyOf(targets));
    }

    @Override
    public @NonNull List<String> suggestions(@NonNull CommandContext<C> context, @NonNull String remaining) {
        return List.of(TOKEN);
    }
}
