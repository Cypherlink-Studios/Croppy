package dev.darkblade.croppy.api.menu;

import java.util.Objects;

/**
 * An alternative item display template gated by an evaluation expression.
 *
 * @param condition expression evaluated against runtime EvaluationContext
 * @param template  the template to display when the condition evaluates to true
 */
public record ConditionalTemplate(
        String condition,
        MenuItemTemplate template
) {

    public ConditionalTemplate {
        Objects.requireNonNull(condition, "condition cannot be null");
        if (condition.isBlank()) {
            throw new IllegalArgumentException("condition cannot be blank");
        }
        Objects.requireNonNull(template, "template cannot be null");
    }
}
