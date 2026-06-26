package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.Optional;

public record SessionizeStage(
                              List<String> fields,
                              Duration gap,
                              Optional<Duration> timeout)
        implements ProcessingStage {}
