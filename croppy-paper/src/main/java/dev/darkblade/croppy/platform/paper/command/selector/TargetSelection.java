package dev.darkblade.croppy.platform.paper.command.selector;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result produced by {@link SelectableTargetParser}, representing either a single
 * explicitly addressed target or a resolved collection of targets matched by an {@code @}-selector.
 *
 * @param rawInput      raw argument text entered by the sender
 * @param selector      true when resolved via an {@code @}-prefixed target selector
 * @param selectorToken the selector token (e.g., {@code "@all"} or {@code "@croppy:soups"}) when {@code selector} is true
 * @param targets       immutable non-empty list of resolved targets
 * @param <T>           domain target type
 */
public record TargetSelection<T>(
        String rawInput,
        boolean selector,
        Optional<String> selectorToken,
        List<T> targets
) {

    public TargetSelection {
        Objects.requireNonNull(rawInput, "rawInput cannot be null");
        selectorToken = (selectorToken == null) ? Optional.empty() : selectorToken;
        targets = (targets == null) ? List.of() : List.copyOf(targets);
    }

    public static <T> TargetSelection<T> single(String rawInput, T target) {
        Objects.requireNonNull(target, "target cannot be null");
        return new TargetSelection<>(rawInput, false, Optional.empty(), List.of(target));
    }

    public static <T> TargetSelection<T> selected(String rawInput, String selectorToken, List<T> targets) {
        Objects.requireNonNull(selectorToken, "selectorToken cannot be null");
        return new TargetSelection<>(rawInput, true, Optional.of(selectorToken), targets);
    }

    public int size() {
        return targets.size();
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public T first() {
        if (targets.isEmpty()) {
            throw new IllegalStateException("TargetSelection contains no targets");
        }
        return targets.get(0);
    }
}
