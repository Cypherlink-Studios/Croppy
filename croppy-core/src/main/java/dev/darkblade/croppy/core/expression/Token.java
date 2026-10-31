package dev.darkblade.croppy.core.expression;

import java.util.Objects;

/**
 * Immutable token record produced by the {@link ExpressionTokenizer}.
 *
 * @param type     the token classification
 * @param lexeme   raw character representation from source string
 * @param literal  parsed literal value (Number, Boolean, String), or null
 * @param position 0-based character offset in input
 */
public record Token(TokenType type, String lexeme, Object literal, int position) {
    public Token {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(lexeme, "lexeme cannot be null");
    }
}
