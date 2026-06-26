package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.stream.Stream;

public record MapStage(List<Assignment> assignments) implements ProcessingStage {
    public MapStage(Assignment... assignments) {
        this(Stream.of(assignments).toList());
    }
}
