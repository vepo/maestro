package dev.vepo.maestro.parser;

import static dev.vepo.maestro.parser.DomainFixtures.query;
import static dev.vepo.maestro.parser.DomainFixtures.streamModel;
import static dev.vepo.maestro.parser.Scenario.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;

class BasicQueriesTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseSimpleQuery() {
        var expected = streamModel(query("input_topic", "output_topic"));
        var parsed = new StreamModel[1];

        given("a Query with one source topic and one sink topic")
            .when("the Stream Language is parsed")
            .then("a StreamModel with the matching Query is produced")
            .run(
                () -> { },
                () -> parsed[0] = parser.parse("FROM input_topic TO output_topic"),
                () -> assertEquals(expected, parsed[0])
            );
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
