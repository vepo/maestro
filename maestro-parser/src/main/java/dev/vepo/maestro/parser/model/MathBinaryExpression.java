package dev.vepo.maestro.parser.model;

public record MathBinaryExpression(Expression left, MathOperator operator, Expression right) implements Expression {}
