package dev.vepo.maestro.parser.model;

public record RegexPredicate(String fieldName, String pattern) implements Expression {}
