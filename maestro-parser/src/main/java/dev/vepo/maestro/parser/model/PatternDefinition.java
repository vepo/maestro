package dev.vepo.maestro.parser.model;

import java.util.Optional;

public record PatternDefinition(
                                String name,
                                Expression expression,
                                Optional<Duration> within,
                                Optional<String> afterPattern) {}
