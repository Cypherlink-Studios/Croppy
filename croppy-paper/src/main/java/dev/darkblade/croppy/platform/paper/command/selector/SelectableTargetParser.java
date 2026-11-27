package dev.darkblade.croppy.platform.paper.command.selector;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Composable Cloud 2.0 {@link ArgumentParser} and {@link SuggestionProvider} resolving
 * either an {@code @}-prefixed target selector via ordered {@link SelectorRule}s or a
 * single target via {@link SingleTargetResolver}.
 *
 * @param <C> command sender context type
 * @param <T> domain target type
 */
public final class SelectableTargetParser<C, T>
        implements ArgumentParser<C, TargetSelection<T>>, SuggestionProvider<C> {

    private final List<SelectorRule<C, T>> rules;
    private final SingleTargetResolver<C, T> singleResolver;
    private final BiFunction<CommandContext<C>, CommandInput, List<String>> singleSuggestions;
    private final Function<String, String> unknownSelectorMessage;
    private final Function<String, String> emptySelectorMessage;

    private SelectableTargetParser(Builder<C, T> builder) {
        this.rules = List.copyOf(builder.rules);
        this.singleResolver = Objects.requireNonNull(builder.singleResolver, "singleResolver cannot be null");
        this.singleSuggestions = builder.singleSuggestions != null
                ? builder.singleSuggestions
                : (ctx, in) -> List.of();
        this.unknownSelectorMessage = builder.unknownSelectorMessage != null
                ? builder.unknownSelectorMessage
                : token -> "Unknown target selector: '" + token + "'";
        this.emptySelectorMessage = builder.emptySelectorMessage != null
                ? builder.emptySelectorMessage
                : token -> "No targets matched selector: '" + token + "'";
    }

    public static <C, T> Builder<C, T> builder() {
        return new Builder<>();
    }

    public ParserDescriptor<C, TargetSelection<T>> descriptor(TypeToken<TargetSelection<T>> typeToken) {
        Objects.requireNonNull(typeToken, "typeToken cannot be null");
        return ParserDescriptor.of(this, typeToken);
    }

    @Override
    public @NonNull ArgumentParseResult<TargetSelection<T>> parse(
            @NonNull CommandContext<C> commandContext,
            @NonNull CommandInput commandInput
    ) {
        String input = commandInput.readString();
        if (input.isBlank()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Target argument cannot be empty"));
        }

        if (input.startsWith("@")) {
            for (SelectorRule<C, T> rule : rules) {
                if (rule.matches(input)) {
                    ArgumentParseResult<List<T>> ruleResult = rule.resolve(commandContext, input);
                    if (ruleResult.failure().isPresent()) {
                        return ArgumentParseResult.failure(ruleResult.failure().get());
                    }
                    List<T> resolved = ruleResult.parsedValue().orElse(List.of());
                    if (resolved.isEmpty()) {
                        return ArgumentParseResult.failure(
                                new IllegalArgumentException(emptySelectorMessage.apply(input))
                        );
                    }
                    return ArgumentParseResult.success(TargetSelection.selected(input, input, resolved));
                }
            }
            return ArgumentParseResult.failure(
                    new IllegalArgumentException(unknownSelectorMessage.apply(input))
            );
        }

        ArgumentParseResult<T> singleResult = singleResolver.resolve(commandContext, input);
        if (singleResult.failure().isPresent()) {
            return ArgumentParseResult.failure(singleResult.failure().get());
        }
        T resolved = singleResult.parsedValue().orElse(null);
        if (resolved == null) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Unknown target: '" + input + "'"));
        }
        return ArgumentParseResult.success(TargetSelection.single(input, resolved));
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
        String remaining = input.peekString().toLowerCase(Locale.ROOT);
        Set<String> candidates = new LinkedHashSet<>();

        for (SelectorRule<C, T> rule : rules) {
            List<String> ruleSuggestions = rule.suggestions(context, remaining);
            if (ruleSuggestions != null) {
                candidates.addAll(ruleSuggestions);
            }
        }

        List<String> singles = singleSuggestions.apply(context, input);
        if (singles != null) {
            candidates.addAll(singles);
        }

        List<Suggestion> filtered = new ArrayList<>();
        for (String candidate : candidates) {
            if (remaining.isEmpty() || candidate.toLowerCase(Locale.ROOT).contains(remaining)) {
                filtered.add(Suggestion.suggestion(candidate));
            }
        }
        return CompletableFuture.completedFuture(filtered);
    }

    public static final class Builder<C, T> {
        private final List<SelectorRule<C, T>> rules = new ArrayList<>();
        private SingleTargetResolver<C, T> singleResolver;
        private BiFunction<CommandContext<C>, CommandInput, List<String>> singleSuggestions;
        private Function<String, String> unknownSelectorMessage;
        private Function<String, String> emptySelectorMessage;

        private Builder() {
        }

        public Builder<C, T> addRule(SelectorRule<C, T> rule) {
            this.rules.add(Objects.requireNonNull(rule, "rule cannot be null"));
            return this;
        }

        public Builder<C, T> singleResolver(SingleTargetResolver<C, T> singleResolver) {
            this.singleResolver = Objects.requireNonNull(singleResolver, "singleResolver cannot be null");
            return this;
        }

        public Builder<C, T> singleSuggestions(Supplier<Collection<String>> supplier) {
            Objects.requireNonNull(supplier, "supplier cannot be null");
            this.singleSuggestions = (ctx, in) -> List.copyOf(supplier.get());
            return this;
        }

        public Builder<C, T> singleSuggestions(BiFunction<CommandContext<C>, CommandInput, List<String>> singleSuggestions) {
            this.singleSuggestions = Objects.requireNonNull(singleSuggestions, "singleSuggestions cannot be null");
            return this;
        }

        public Builder<C, T> unknownSelectorMessage(Function<String, String> unknownSelectorMessage) {
            this.unknownSelectorMessage = Objects.requireNonNull(unknownSelectorMessage, "unknownSelectorMessage cannot be null");
            return this;
        }

        public Builder<C, T> emptySelectorMessage(Function<String, String> emptySelectorMessage) {
            this.emptySelectorMessage = Objects.requireNonNull(emptySelectorMessage, "emptySelectorMessage cannot be null");
            return this;
        }

        public SelectableTargetParser<C, T> build() {
            return new SelectableTargetParser<>(this);
        }
    }
}
