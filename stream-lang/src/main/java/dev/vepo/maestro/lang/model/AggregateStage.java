package dev.vepo.maestro.lang.model;

import java.util.List;
import java.util.stream.Stream;

public record AggregateStage(List<String> groupByFields, List<AggregateFunction> functions) implements ProcessingStage {
    public AggregateStage(List<String> groupByFields, AggregateFunction... functions) {
        this(groupByFields, Stream.of(functions)
                                  .toList());
    }
}