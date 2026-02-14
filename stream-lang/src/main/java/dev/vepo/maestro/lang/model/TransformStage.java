package dev.vepo.maestro.lang.model;

import java.util.List;
import java.util.stream.Stream;

public record TransformStage(List<Assignment> assignments) implements ProcessingStage {
    public TransformStage(Assignment... assignments) {
        this(Stream.of(assignments)
                   .toList());
    }
}