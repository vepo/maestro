package dev.vepo.maestro.lang.model;

import java.util.List;

public record TransformStage(List<Assignment> assignments) implements ProcessingStage {}