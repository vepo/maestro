package dev.vepo.maestro.lang.model;

import java.util.Collections;
import java.util.List;

public record SourcePipeline(SourceStage sourceStage,
                             List<ProcessingStage> processingStages) {
    public SourcePipeline(SourceStage sourceStage) {
        this(sourceStage, Collections.emptyList());
    }
}