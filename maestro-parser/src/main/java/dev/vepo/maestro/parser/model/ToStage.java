package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.stream.Stream;

public record ToStage(List<String> topics) implements ProcessingStage {
    public ToStage(String... topics) {
        this(Stream.of(topics).toList());
    }
}
