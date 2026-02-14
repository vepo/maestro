package dev.vepo.maestro.lang.model;

import java.util.List;
import java.util.stream.Stream;

public record InPredicate(String fieldName, List<Literal> values) implements Expression {
    public InPredicate(String fieldName, Literal... values) {
        this(fieldName, Stream.of(values)
                              .toList());
    }
}