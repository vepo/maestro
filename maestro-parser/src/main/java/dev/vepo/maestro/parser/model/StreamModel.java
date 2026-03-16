package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.stream.Stream;

public record StreamModel(List<Query> queries) {
    public StreamModel(Query... queries) {
        this(Stream.of(queries).toList());
    }
}