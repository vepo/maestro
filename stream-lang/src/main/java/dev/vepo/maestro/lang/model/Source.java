package dev.vepo.maestro.lang.model;

import java.util.List;

import dev.vepo.maestro.lang.model.predicate.NoPredicate;
import dev.vepo.maestro.lang.model.predicate.Predicate;

public record Source(List<String> topic, Predicate predicate) {
    public Source(List<String> topic) {
        this(topic, new NoPredicate());
    }
}
