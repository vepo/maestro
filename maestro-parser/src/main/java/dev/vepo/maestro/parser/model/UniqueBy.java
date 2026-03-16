package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.stream.Stream;

public record UniqueBy(List<String> fieldNames) {
    public UniqueBy(String... fieldNames) {
        this(Stream.of(fieldNames)
                   .toList());
    }
}
