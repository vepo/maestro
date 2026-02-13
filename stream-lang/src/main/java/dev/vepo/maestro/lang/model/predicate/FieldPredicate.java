package dev.vepo.maestro.lang.model.predicate;

public record FieldPredicate(String field, Operator operator, Value value) implements Predicate {}
