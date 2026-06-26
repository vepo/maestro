package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.Optional;

public record ProjectStage(Optional<String> outputAlias, List<ProjectField> fields) implements ProcessingStage {
    public ProjectStage(List<String> fieldNames) {
        this(Optional.empty(), fieldNames.stream().map(ProjectField::new).toList());
    }
}
