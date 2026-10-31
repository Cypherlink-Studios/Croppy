package dev.darkblade.croppy.core.expression;

import dev.darkblade.croppy.api.expression.Expression;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compiles string expression definitions into evaluated {@link ExpressionNode} abstract syntax trees.
 * Automatically caches compiled expressions for zero-allocation runtime reuse.
 */
public final class ExpressionCompiler {

    private static final Map<String, ExpressionNode> CACHE = new ConcurrentHashMap<>();

    private final List<Token> tokens;
    private int current = 0;

    private ExpressionCompiler(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Compiles an expression string into an {@link ExpressionNode}, reusing cached trees when available.
     *
     * @param expression the raw expression string
     * @return the compiled expression node
     * @throws IllegalArgumentException if the expression syntax is invalid
     */
    public static ExpressionNode compile(String expression) {
        Objects.requireNonNull(expression, "expression cannot be null");
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            return new LiteralNode(Boolean.TRUE);
        }

        return CACHE.computeIfAbsent(trimmed, ExpressionCompiler::compileDirect);
    }

    /**
     * Compiles an expression string directly without accessing the global cache.
     *
     * @param expression the raw expression string
     * @return the newly compiled expression node
     */
    public static ExpressionNode compileDirect(String expression) {
        List<Token> tokens = new ExpressionTokenizer(expression).tokenize();
        ExpressionCompiler parser = new ExpressionCompiler(tokens);
        ExpressionNode node = parser.parseExpression();

        if (!parser.isAtEnd()) {
            Token unexpected = parser.peek();
            throw new IllegalArgumentException("Unexpected token '" + unexpected.lexeme() + "' at position " + unexpected.position());
        }

        return node;
    }

    /**
     * Clears the compilation cache.
     */
    public static void clearCache() {
        CACHE.clear();
    }

    // --- Parser Grammar Hierarchy ---

    private ExpressionNode parseExpression() {
        return parseOr();
    }

    private ExpressionNode parseOr() {
        ExpressionNode expr = parseAnd();

        while (match(TokenType.OR)) {
            Token op = previous();
            ExpressionNode right = parseAnd();
            expr = new LogicalNode(expr, op.type(), right);
        }

        return expr;
    }

    private ExpressionNode parseAnd() {
        ExpressionNode expr = parseComparison();

        while (match(TokenType.AND)) {
            Token op = previous();
            ExpressionNode right = parseComparison();
            expr = new LogicalNode(expr, op.type(), right);
        }

        return expr;
    }

    private ExpressionNode parseComparison() {
        ExpressionNode expr = parseTerm();

        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL, TokenType.LESS_THAN,
                TokenType.LESS_THAN_OR_EQUAL, TokenType.GREATER_THAN,
                TokenType.GREATER_THAN_OR_EQUAL, TokenType.CONTAINS, TokenType.IN)) {
            Token op = previous();
            ExpressionNode right = parseTerm();
            expr = new ComparisonNode(expr, op.type(), right);
        }

        return expr;
    }

    private ExpressionNode parseTerm() {
        ExpressionNode expr = parseFactor();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            ExpressionNode right = parseFactor();
            expr = new ArithmeticNode(expr, op.type(), right);
        }

        return expr;
    }

    private ExpressionNode parseFactor() {
        ExpressionNode expr = parseUnary();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token op = previous();
            ExpressionNode right = parseUnary();
            expr = new ArithmeticNode(expr, op.type(), right);
        }

        return expr;
    }

    private ExpressionNode parseUnary() {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            Token op = previous();
            ExpressionNode target = parseUnary();
            return new UnaryNode(op.type(), target);
        }

        return parsePrimary();
    }

    private ExpressionNode parsePrimary() {
        if (match(TokenType.NUMBER, TokenType.STRING, TokenType.BOOLEAN)) {
            return new LiteralNode(previous().literal());
        }

        if (match(TokenType.IDENTIFIER)) {
            return new VariableNode(previous().lexeme());
        }

        if (match(TokenType.LEFT_PAREN)) {
            ExpressionNode expr = parseExpression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after grouped expression");
            return expr;
        }

        Token token = peek();
        throw new IllegalArgumentException("Unexpected token '" + token.lexeme() + "' at position " + token.position() + " while parsing primary expression");
    }

    // --- Helper Methods ---

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String errorMessage) {
        if (check(type)) {
            return advance();
        }
        Token currentToken = peek();
        throw new IllegalArgumentException(errorMessage + " but found '" + currentToken.lexeme() + "' at position " + currentToken.position());
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
