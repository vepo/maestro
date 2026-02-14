package dev.vepo.maestro.lang.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public record SourcePipeline(SourceStage sourceStage,
                             List<ProcessingStage> processingStages) {
    public SourcePipeline(SourceStage sourceStage) {
        this(sourceStage, Collections.emptyList());
    }

    public SourcePipeline(SourceStage sourceStage, ProcessingStage... processingStages) {
        this(sourceStage, Stream.of(processingStages)
                                .toList());
    }
}