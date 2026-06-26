package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.Optional;

public record BranchCase(Optional<Expression> condition, List<ProcessingStage> stages) {
    public static BranchCase when(Expression condition, List<ProcessingStage> stages) {
        return new BranchCase(Optional.of(condition), stages);
    }

    public static BranchCase defaults(List<ProcessingStage> stages) {
        return new BranchCase(Optional.empty(), stages);
    }
}
