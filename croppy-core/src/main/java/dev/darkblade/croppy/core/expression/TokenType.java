package dev.darkblade.croppy.core.expression;

/**
 * Enumeration of token types recognized by the expression tokenizer.
 */
public enum TokenType {
    // Literals
    NUMBER,
    STRING,
    BOOLEAN,
    IDENTIFIER,

    // Comparison Operators
    EQUAL,                  // ==
    NOT_EQUAL,              // !=
    LESS_THAN,              // <
    LESS_THAN_OR_EQUAL,     // <=
    GREATER_THAN,           // >
    GREATER_THAN_OR_EQUAL,  // >=
    CONTAINS,               // contains
    IN,                     // in

    // Logical Operators
    AND,                    // &&
    OR,                     // ||
    NOT,                    // !

    // Arithmetic Operators
    PLUS,                   // +
    MINUS,                  // -
    STAR,                   // *
    SLASH,                  // /

    // Grouping
    LEFT_PAREN,             // (
    RIGHT_PAREN,            // )

    // End of input
    EOF
}
