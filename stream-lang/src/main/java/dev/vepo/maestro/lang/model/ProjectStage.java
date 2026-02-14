package dev.vepo.maestro.lang.model;

import java.util.List;

public record ProjectStage(List<String> fields) implements ProcessingStage {}