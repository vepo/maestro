package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.stream.Stream;

public record GroupByStage(List<String> fields) implements ProcessingStage {
    public GroupByStage(String... fields) {
        this(Stream.of(fields).toList());
    }
}
