package dev.vepo.maestro.lang.model;

import java.util.List;

public record AggregateStage(List<String> groupByFields, List<AggregateFunction> functions) implements ProcessingStage {}