package dev.vepo.maestro.parser.model;

public sealed interface ProcessingStage
        permits ProjectStage, AggregateStage, WindowStage, JoinStage, FilterStage, MapStage,
        GroupByStage, ToStage, BranchStage, PatternStage, SessionizeStage {}
