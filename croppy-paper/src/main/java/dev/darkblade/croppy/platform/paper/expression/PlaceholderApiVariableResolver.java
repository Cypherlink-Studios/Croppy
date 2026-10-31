package dev.darkblade.croppy.platform.paper.expression;

import dev.darkblade.croppy.api.expression.EvaluationContext;
import dev.darkblade.croppy.api.expression.VariableResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter implementing {@link VariableResolver} to bridge Minecraft's PlaceholderAPI
 * placeholders (enclosed in '%...%') into the pure expression engine.
 */
public final class PlaceholderApiVariableResolver implements VariableResolver {

    private static final boolean PAPI_PRESENT;
    private static final MethodHandle SET_PLACEHOLDERS_HANDLE;

    static {
        boolean present = false;
        MethodHandle handle = null;
        try {
            Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = clazz.getMethod("setPlaceholders", Player.class, String.class);
            handle = MethodHandles.lookup().unreflect(method);
            present = true;
        } catch (Throwable ignored) {
            // PlaceholderAPI not installed on server
        }
        PAPI_PRESENT = present;
        SET_PLACEHOLDERS_HANDLE = handle;
    }

    @Override
    public Optional<Object> resolve(String name, EvaluationContext context) {
        Objects.requireNonNull(name, "name cannot be null");

        if (!name.startsWith("%") || !name.endsWith("%") || name.length() <= 2) {
            return Optional.empty();
        }

        if (!PAPI_PRESENT || SET_PLACEHOLDERS_HANDLE == null) {
            return Optional.empty();
        }

        // Check if Bukkit plugin is enabled
        try {
            if (Bukkit.getPluginManager() == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                return Optional.empty();
            }
        } catch (Throwable ignored) {
            // Occurs in mock/unit test environments without Bukkit runtime
            return Optional.empty();
        }

        Object playerObj = context.getVariable("player").orElse(null);
        if (!(playerObj instanceof Player player)) {
            return Optional.empty();
        }

        try {
            String resolved = (String) SET_PLACEHOLDERS_HANDLE.invoke(player, name);
            if (resolved == null || resolved.equals(name)) {
                return Optional.empty();
            }
            return Optional.of(parseTypedValue(resolved));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    /**
     * Checks whether PlaceholderAPI is available in the current runtime environment.
     *
     * @return true if PlaceholderAPI classes and plugin are present
     */
    public static boolean isAvailable() {
        return PAPI_PRESENT;
    }

    private static Object parseTypedValue(String value) {
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }
}
