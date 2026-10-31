package dev.darkblade.croppy.api.menu;

import java.util.Objects;
import java.util.Optional;

/**
 * An executable action triggered upon clicking an interactive menu slot.
 *
 * @param click     click filter governing when the action executes
 * @param type      category of operation to execute
 * @param target    target payload (command, action identifier, sound, message)
 * @param condition optional condition expression that must evaluate to true
 */
public record MenuAction(
        ClickTypeFilter click,
        MenuActionType type,
        String target,
        String condition
) {

    public MenuAction {
        click = (click == null) ? ClickTypeFilter.ALL : click;
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        condition = (condition != null && condition.isBlank()) ? null : condition;
    }

    /**
     * Creates an unconditional menu action triggered by any click type.
     *
     * @param type   action type
     * @param target action target
     * @return constructed action
     */
    public static MenuAction of(MenuActionType type, String target) {
        return new MenuAction(ClickTypeFilter.ALL, type, target, null);
    }

    /**
     * Returns the optional condition expression.
     *
     * @return optional condition
     */
    public Optional<String> getCondition() {
        return Optional.ofNullable(condition);
    }
}
