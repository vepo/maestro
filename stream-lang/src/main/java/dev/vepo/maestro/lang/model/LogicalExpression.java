package dev.vepo.maestro.lang.model;

public record LogicalExpression(Expression left, LogicalOperator operator, Expression right) 
    implements Expression {}
