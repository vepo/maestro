package dev.vepo.maestro.parser.model;

public record FilterStage(Expression condition) implements ProcessingStage {}