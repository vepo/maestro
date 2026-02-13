package dev.vepo.maestro.lang.model.predicate;

public record AndPredicate(Predicate left, Predicate right) implements Predicate {}
