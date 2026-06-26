package dev.vepo.maestro.parser.model;

import java.util.List;
import java.util.stream.Stream;

public record Query(SourcePipeline sourcePipeline,
                    List<String> sinkTopics,
                    QuerySettings settings) {
    public Query(SourcePipeline sourcePipeline, List<String> sinkTopics) {
        this(sourcePipeline, sinkTopics, QuerySettings.empty());
    }

    public Query(SourcePipeline sourcePipeline, String... sinkTopics) {
        this(sourcePipeline, Stream.of(sinkTopics).toList(), QuerySettings.empty());
    }

    public Query(SourcePipeline sourcePipeline, QuerySettings settings, String... sinkTopics) {
        this(sourcePipeline, Stream.of(sinkTopics).toList(), settings);
    }
}