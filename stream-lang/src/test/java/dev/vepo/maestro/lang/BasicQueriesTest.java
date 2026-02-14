package dev.vepo.maestro.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;

class BasicQueriesTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseSimpleQuery() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("input_topic")),
                                               "output_topic")),
                     parser.parse("FROM input_topic TO output_topic"));
    }

    @Test
    void shouldParseQueryWithMultipleSourceTopics() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("topic1", "topic2", "topic3")),
                                               "output_topic")),
                     parser.parse("FROM topic1, topic2, topic3 TO output_topic"));
    }

    @Test
    void shouldParseQueryWithMultipleSinkTopics() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("input_topic")),
                                               "output1", "output2", "output3")),
                     parser.parse("FROM input_topic TO output1, output2, output3"));
    }

    @Test
    void shouldParseMultipleQueries() {
        var expected = new StreamModel(new Query(new SourcePipeline(new SourceStage("topic1")), "sink1"),
                                       new Query(new SourcePipeline(new SourceStage("topic2")), "sink2"));
        assertEquals(expected,
                     parser.parse("FROM topic1 TO sink1; FROM topic2 TO sink2"));
    }
}
