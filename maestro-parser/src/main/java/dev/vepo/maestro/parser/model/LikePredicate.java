package dev.vepo.maestro.parser.model;

public record LikePredicate(String fieldName, String pattern) implements Expression {}