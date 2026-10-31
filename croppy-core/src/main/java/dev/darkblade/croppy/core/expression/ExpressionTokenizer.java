package dev.darkblade.croppy.core.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lexical analyzer converting expression strings into a stream of {@link Token} instances.
 */
public final class ExpressionTokenizer {

    private final String source;
    private final int length;
    private int cursor = 0;

    public ExpressionTokenizer(String source) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.length = source.length();
    }

    /**
     * Scans and returns all tokens from the source string.
     *
     * @return list of tokens ending with an EOF token
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (cursor < length) {
            char c = peek();

            if (Character.isWhitespace(c)) {
                cursor++;
                continue;
            }

            int startPos = cursor;

            switch (c) {
                case '(' -> {
                    cursor++;
                    tokens.add(new Token(TokenType.LEFT_PAREN, "(", null, startPos));
                }
                case ')' -> {
                    cursor++;
                    tokens.add(new Token(TokenType.RIGHT_PAREN, ")", null, startPos));
                }
                case '+' -> {
                    cursor++;
                    tokens.add(new Token(TokenType.PLUS, "+", null, startPos));
                }
                case '-' -> {
                    cursor++;
                    tokens.add(new Token(TokenType.MINUS, "-", null, startPos));
                }
                case '*' -> {
                    cursor++;
                    tokens.add(new Token(TokenType.STAR, "*", null, startPos));
                }
                case '/' -> {
                    cursor++;
                    tokens.add(new Token(TokenType.SLASH, "/", null, startPos));
                }
                case '!' -> {
                    cursor++;
                    if (match('=')) {
                        tokens.add(new Token(TokenType.NOT_EQUAL, "!=", null, startPos));
                    } else {
                        tokens.add(new Token(TokenType.NOT, "!", null, startPos));
                    }
                }
                case '=' -> {
                    cursor++;
                    if (match('=')) {
                        tokens.add(new Token(TokenType.EQUAL, "==", null, startPos));
                    } else {
                        // Allow single = as relaxed equality in config expressions
                        tokens.add(new Token(TokenType.EQUAL, "=", null, startPos));
                    }
                }
                case '<' -> {
                    cursor++;
                    if (match('=')) {
                        tokens.add(new Token(TokenType.LESS_THAN_OR_EQUAL, "<=", null, startPos));
                    } else {
                        tokens.add(new Token(TokenType.LESS_THAN, "<", null, startPos));
                    }
                }
                case '>' -> {
                    cursor++;
                    if (match('=')) {
                        tokens.add(new Token(TokenType.GREATER_THAN_OR_EQUAL, ">=", null, startPos));
                    } else {
                        tokens.add(new Token(TokenType.GREATER_THAN, ">", null, startPos));
                    }
                }
                case '&' -> {
                    cursor++;
                    if (match('&')) {
                        tokens.add(new Token(TokenType.AND, "&&", null, startPos));
                    } else {
                        throw new IllegalArgumentException("Unexpected character '&' at position " + startPos + ". Did you mean '&&'?");
                    }
                }
                case '|' -> {
                    cursor++;
                    if (match('|')) {
                        tokens.add(new Token(TokenType.OR, "||", null, startPos));
                    } else {
                        throw new IllegalArgumentException("Unexpected character '|' at position " + startPos + ". Did you mean '||'?");
                    }
                }
                case '\'', '"' -> tokens.add(scanString(c, startPos));
                default -> {
                    if (Character.isDigit(c)) {
                        tokens.add(scanNumber(startPos));
                    } else if (isIdentifierStart(c)) {
                        tokens.add(scanIdentifier(startPos));
                    } else {
                        throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + startPos);
                    }
                }
            }
        }

        tokens.add(new Token(TokenType.EOF, "", null, cursor));
        return List.copyOf(tokens);
    }

    private Token scanString(char quote, int startPos) {
        cursor++; // Skip opening quote
        int contentStart = cursor;

        while (cursor < length && peek() != quote) {
            cursor++;
        }

        if (cursor >= length) {
            throw new IllegalArgumentException("Unterminated string literal starting at position " + startPos);
        }

        String literal = source.substring(contentStart, cursor);
        cursor++; // Skip closing quote
        String lexeme = source.substring(startPos, cursor);
        return new Token(TokenType.STRING, lexeme, literal, startPos);
    }

    private Token scanNumber(int startPos) {
        while (cursor < length && Character.isDigit(peek())) {
            cursor++;
        }

        if (cursor < length && peek() == '.' && cursor + 1 < length && Character.isDigit(source.charAt(cursor + 1))) {
            cursor++; // Consume decimal point
            while (cursor < length && Character.isDigit(peek())) {
                cursor++;
            }
        }

        String lexeme = source.substring(startPos, cursor);
        Double value = Double.parseDouble(lexeme);
        return new Token(TokenType.NUMBER, lexeme, value, startPos);
    }

    private Token scanIdentifier(int startPos) {
        while (cursor < length && isIdentifierPart(peek())) {
            cursor++;
        }

        String lexeme = source.substring(startPos, cursor);
        String lower = lexeme.toLowerCase();

        return switch (lower) {
            case "true" -> new Token(TokenType.BOOLEAN, lexeme, Boolean.TRUE, startPos);
            case "false" -> new Token(TokenType.BOOLEAN, lexeme, Boolean.FALSE, startPos);
            case "and" -> new Token(TokenType.AND, lexeme, null, startPos);
            case "or" -> new Token(TokenType.OR, lexeme, null, startPos);
            case "not" -> new Token(TokenType.NOT, lexeme, null, startPos);
            case "in" -> new Token(TokenType.IN, lexeme, null, startPos);
            case "contains" -> new Token(TokenType.CONTAINS, lexeme, null, startPos);
            default -> new Token(TokenType.IDENTIFIER, lexeme, lexeme, startPos);
        };
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '%' || c == '@';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == ':' || c == '-' || c == '%';
    }

    private char peek() {
        return source.charAt(cursor);
    }

    private boolean match(char expected) {
        if (cursor < length && source.charAt(cursor) == expected) {
            cursor++;
            return true;
        }
        return false;
    }
}
