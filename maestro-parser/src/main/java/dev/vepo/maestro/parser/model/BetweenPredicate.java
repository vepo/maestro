package dev.vepo.maestro.parser.model;

public record BetweenPredicate(String fieldName, Literal lowerBound, Literal upperBound) 
    implements Expression {}
