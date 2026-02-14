package dev.vepo.maestro.lang.model;

import java.util.List;

import dev.vepo.maestro.lang.model.predicate.NoPredicate;
import dev.vepo.maestro.lang.model.predicate.Predicate;

public record Source(List<String> topic, Predicate predicate, UniqueBy unique) {
    public Source(List<String> topic) {
        this(topic, new NoPredicate(), UniqueBy.EMPTY);
    }

    public Source(String topic, UniqueBy unique) {
        this(List.of(topic), new NoPredicate(), unique);
    }

    public Source(String topic) {
        this(List.of(topic), new NoPredicate(), UniqueBy.EMPTY);
    }

    public Source(String topic, Predicate predicate) {
        this(List.of(topic), predicate, UniqueBy.EMPTY);
    }
}
