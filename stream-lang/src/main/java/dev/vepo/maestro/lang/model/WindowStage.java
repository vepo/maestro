package dev.vepo.maestro.lang.model;

import java.util.Optional;

public record WindowStage(WindowType windowType, Duration windowSize, Optional<Duration> slideInterval) 
    implements ProcessingStage {}