package dev.vepo.maestro.lang.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record SourceStage(List<String> topics,
                          Optional<Expression> whereClause,
                          Optional<UniqueBy> uniqueBy) {
    public SourceStage(List<String> topics) {
        this(topics, Optional.empty(), Optional.empty());
    }

    public SourceStage(String topic, UniqueBy uniqueBy) {
        this(List.of(topic),
             Optional.empty(),
             Optional.of(requireNonNull(uniqueBy, "'uniqueBy' cannot be null!")));
    }

    public SourceStage(String topic, Expression whereClause) {
        this(List.of(topic),
             Optional.of(requireNonNull(whereClause, "'whereClause' cannot be null!")),
             Optional.empty());
    }

    public SourceStage(String... topics) {
        this(Stream.of(topics).toList(), Optional.empty(), Optional.empty());
    }
}