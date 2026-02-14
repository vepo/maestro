package dev.vepo.maestro.lang.model;

public record RegexPredicate(String fieldName, String pattern) implements Expression {}
