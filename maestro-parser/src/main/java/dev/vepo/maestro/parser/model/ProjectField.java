package dev.vepo.maestro.parser.model;

import java.util.Optional;

public record ProjectField(String name, Optional<Expression> expression) {
    public ProjectField(String name) {
        this(name, Optional.empty());
    }
}
