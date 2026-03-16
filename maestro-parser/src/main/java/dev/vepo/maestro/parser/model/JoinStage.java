package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.Optional;

public record JoinStage(List<String> sourceTopics, JoinCondition condition, Optional<WindowType> windowType) 
    implements ProcessingStage {}
