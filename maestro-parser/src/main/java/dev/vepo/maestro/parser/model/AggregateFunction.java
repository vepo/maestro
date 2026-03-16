package dev.vepo.maestro.parser.model;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

public record AggregateFunction(AggregateFunctionType type, String field, Optional<String> alias) {
    public enum AggregateFunctionType {
        COUNT, SUM, AVG, MIN, MAX, FIRST, LAST
    }

    public AggregateFunction(AggregateFunctionType type, String field, String alias) {
        this(type, field, Optional.of(requireNonNull(alias, "'alias' cannot be null!")));
    }
}
