package dev.vepo.maestro.parser.model;

public sealed interface Literal permits FieldReferenceLiteral, StringLiteral, NumberLiteral, BooleanLiteral, NullLiteral {
    public static Literal number(String value) {
        return new NumberLiteral(value);
    }

    public static Literal string(String value) {
        return new StringLiteral(value);
    }
}