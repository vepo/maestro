package dev.vepo.maestro.lang.model;

import java.util.List;
import java.util.stream.Stream;

public record Query(SourcePipeline sourcePipeline,
                    List<String> sinkTopics) {
    public Query(SourcePipeline sourcePipeline, String... sinkTopics) {
        this(sourcePipeline, Stream.of(sinkTopics)
                                   .toList());
    }
}