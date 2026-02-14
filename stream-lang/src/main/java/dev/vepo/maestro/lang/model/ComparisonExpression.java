package dev.vepo.maestro.lang.model;

public record ComparisonExpression(Expression left, ComparisonOperator operator, Expression right) 
    implements Expression {}