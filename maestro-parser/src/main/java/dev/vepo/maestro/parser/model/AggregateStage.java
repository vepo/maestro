package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.stream.Stream;

public record AggregateStage(List<AggregateFunction> functions) implements ProcessingStage {
    public AggregateStage(AggregateFunction... functions) {
        this(Stream.of(functions).toList());
    }
}
