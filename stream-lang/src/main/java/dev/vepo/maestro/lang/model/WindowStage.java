package dev.vepo.maestro.lang.model;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

public record WindowStage(WindowType windowType, Duration windowSize, Optional<Duration> slideInterval) implements ProcessingStage {

    public WindowStage(WindowType windowType, Duration windowSize) {
        this(windowType, windowSize, Optional.empty());
    }

    public WindowStage(WindowType windowType, Duration windowSize, Duration slideInterval) {
        this(windowType, windowSize, Optional.of(requireNonNull(slideInterval, "'slideInterval' cannot be null!")));
    }
}