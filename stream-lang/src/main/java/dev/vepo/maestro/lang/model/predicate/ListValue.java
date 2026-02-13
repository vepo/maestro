package dev.vepo.maestro.lang.model.predicate;

import java.util.List;
import java.util.stream.Stream;

public record ListValue(List<Value> values) implements Value {
    public ListValue(Value... values) {
        this(Stream.of(values)
                   .toList());
    }
}
