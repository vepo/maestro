package dev.vepo.maestro.parser.model;

import java.util.List;

public record BranchStage(List<BranchCase> cases) implements ProcessingStage {}
