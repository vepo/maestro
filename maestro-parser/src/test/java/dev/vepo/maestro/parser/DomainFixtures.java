package dev.vepo.maestro.parser;

import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;

/**
 * Domain-language builders for expected StreamModel values in tests.
 */
final class DomainFixtures {

    private DomainFixtures() {
    }

    static StreamModel streamModel(Query... queries) {
        return new StreamModel(queries);
    }

    static Query query(String sourceTopic, String... sinkTopics) {
        return new Query(sourcePipeline(sourceTopic), sinkTopics);
    }

    static Query query(SourcePipeline sourcePipeline, String... sinkTopics) {
        return new Query(sourcePipeline, sinkTopics);
    }

    static SourcePipeline sourcePipeline(String... sourceTopics) {
        return new SourcePipeline(new SourceStage(sourceTopics));
    }
}
