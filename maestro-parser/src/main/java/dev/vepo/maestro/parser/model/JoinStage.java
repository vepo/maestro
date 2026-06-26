package dev.vepo.maestro.parser.model;

import java.util.Optional;

import dev.vepo.maestro.parser.model.Duration;

public record JoinStage(
                        String target,
                        Expression condition,
                        JoinKind kind,
                        Optional<String> sourceTopic,
                        Optional<Duration> within)
        implements ProcessingStage {}
