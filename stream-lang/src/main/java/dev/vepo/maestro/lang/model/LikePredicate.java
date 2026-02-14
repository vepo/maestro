package dev.vepo.maestro.lang.model;

public record LikePredicate(String fieldName, String pattern) implements Expression {}