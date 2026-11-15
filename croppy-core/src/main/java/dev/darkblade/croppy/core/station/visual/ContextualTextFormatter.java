package dev.darkblade.croppy.core.station.visual;

import dev.darkblade.croppy.api.expression.EvaluationContext;

/**
 * Single-pass contextual template formatter that resolves {@code {key}} placeholders
 * dynamically against an {@link EvaluationContext}.
 * Supports component capability keys ({@code {thermal.temp_c}}, {@code {heat.fuel_ticks}}),
 * typed variables ({@code {var.cleanliness}}, {@code {var.tank.fluid}}), and legacy display tokens.
 */
public final class ContextualTextFormatter {

    private ContextualTextFormatter() {
    }

    /**
     * Resolves {@code {token}} placeholders in {@code template} using values from {@code context}.
     * Unresolved tokens are preserved literally so non-variable braces are never stripped.
     *
     * @param template MiniMessage or plain-text template containing {@code {key}} tokens
     * @param context  evaluation context holding namespaced properties and variables
     * @return formatted text, or empty string if {@code template} is null or empty
     */
    public static String format(String template, EvaluationContext context) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        EvaluationContext ctx = context != null ? context : EvaluationContext.empty();
        int len = template.length();
        StringBuilder sb = new StringBuilder(len);
        int i = 0;

        while (i < len) {
            int openIdx = template.indexOf('{', i);
            if (openIdx < 0) {
                sb.append(template, i, len);
                break;
            }
            sb.append(template, i, openIdx);

            int closeIdx = findClosingBrace(template, openIdx + 1);
            if (closeIdx < 0) {
                // No valid closing brace (or another '{' appeared first): emit literally and continue
                int nextOpen = template.indexOf('{', openIdx + 1);
                if (nextOpen < 0) {
                    sb.append(template, openIdx, len);
                    break;
                }
                sb.append(template, openIdx, nextOpen);
                i = nextOpen;
                continue;
            }

            String key = template.substring(openIdx + 1, closeIdx).trim();
            if (key.isEmpty()) {
                sb.append(template, openIdx, closeIdx + 1);
            } else {
                String resolved = ctx.getVariable(key)
                        .map(ContextualTextFormatter::formatValue)
                        .orElse(null);
                if (resolved != null) {
                    sb.append(resolved);
                } else {
                    sb.append(template, openIdx, closeIdx + 1);
                }
            }
            i = closeIdx + 1;
        }
        return sb.toString();
    }

    private static int findClosingBrace(String s, int fromIndex) {
        for (int idx = fromIndex; idx < s.length(); idx++) {
            char c = s.charAt(idx);
            if (c == '}') {
                return idx;
            }
            if (c == '{') {
                return -1;
            }
        }
        return -1;
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String str) {
            return str;
        }
        return String.valueOf(value);
    }
}
