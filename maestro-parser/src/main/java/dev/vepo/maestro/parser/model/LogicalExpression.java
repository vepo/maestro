package dev.vepo.maestro.parser.model;

public record LogicalExpression(Expression left, LogicalOperator operator, Expression right) 
    implements Expression {}
