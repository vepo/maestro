package dev.vepo.maestro.parser.model;

import java.util.List;

public record ProjectStage(List<String> fields) implements ProcessingStage {}