package dev.vepo.maestro.parser.model;

public record ComparisonExpression(Expression left, ComparisonOperator operator, Expression right) 
    implements Expression {}