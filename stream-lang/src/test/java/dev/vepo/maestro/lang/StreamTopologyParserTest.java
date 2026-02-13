package dev.vepo.maestro.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.Sink;
import dev.vepo.maestro.lang.model.Source;
import dev.vepo.maestro.lang.model.StreamQuery;
import dev.vepo.maestro.lang.model.predicate.FieldPredicate;
import dev.vepo.maestro.lang.model.predicate.Operator;
import dev.vepo.maestro.lang.model.predicate.StringValue;

class StreamTopologyParserTest {

    @Test
    void parserTest() {
        var parser = new StreamTopologyParser();
        assertEquals(List.of(new StreamQuery(new Source(List.of("input_topic")), new Sink(List.of("output_topic")))),
                     parser.parse("FROM input_topic TO output_topic"));
        assertEquals(List.of(new StreamQuery(new Source(List.of("user_events"),
                                                        new FieldPredicate("status", Operator.EQUAL, new StringValue("active"))),
                                             new Sink(List.of("processed_topic", "analytics_topic")))),
                     parser.parse("FROM user_events WHERE status = \"active\" TO processed_topic, analytics_topic"));
    }
}
