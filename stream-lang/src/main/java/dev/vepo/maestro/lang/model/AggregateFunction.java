package dev.vepo.maestro.lang.model;

import java.util.Optional;

public record AggregateFunction(AggregateFunctionType type, String field, Optional<String> alias) {
    public enum AggregateFunctionType { COUNT, SUM, AVG, MIN, MAX, FIRST, LAST }
}
