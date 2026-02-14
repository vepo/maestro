package dev.vepo.maestro.lang.model;

public sealed interface ProcessingStage 
    permits ProjectStage, AggregateStage, WindowStage, JoinStage, FlattenStage, FilterStage, TransformStage {}