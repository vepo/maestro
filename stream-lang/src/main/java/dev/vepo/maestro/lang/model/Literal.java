package dev.vepo.maestro.lang.model;

public sealed interface Literal 
    permits StringLiteral, NumberLiteral, BooleanLiteral, NullLiteral {}