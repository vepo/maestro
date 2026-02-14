package dev.vepo.maestro.lang.model;

public record FilterStage(Expression condition) implements ProcessingStage {}