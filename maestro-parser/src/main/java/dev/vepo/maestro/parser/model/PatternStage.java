package dev.vepo.maestro.parser.model;

import java.util.List;

public record PatternStage(List<PatternDefinition> definitions, String detectAlias) implements ProcessingStage {}
