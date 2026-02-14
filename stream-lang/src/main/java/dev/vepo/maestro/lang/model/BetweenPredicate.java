package dev.vepo.maestro.lang.model;

public record BetweenPredicate(String fieldName, Literal lowerBound, Literal upperBound) 
    implements Expression {}
