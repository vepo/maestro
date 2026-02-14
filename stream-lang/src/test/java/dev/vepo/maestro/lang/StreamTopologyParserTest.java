package dev.vepo.maestro.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.Sink;
import dev.vepo.maestro.lang.model.Source;
import dev.vepo.maestro.lang.model.StreamQuery;
import dev.vepo.maestro.lang.model.UniqueBy;
import dev.vepo.maestro.lang.model.predicate.AndPredicate;
import dev.vepo.maestro.lang.model.predicate.FieldPredicate;
import dev.vepo.maestro.lang.model.predicate.ListValue;
import dev.vepo.maestro.lang.model.predicate.NotPredicate;
import dev.vepo.maestro.lang.model.predicate.NullValue;
import dev.vepo.maestro.lang.model.predicate.NumberValue;
import dev.vepo.maestro.lang.model.predicate.Operator;
import dev.vepo.maestro.lang.model.predicate.RangeValue;
import dev.vepo.maestro.lang.model.predicate.StringValue;

class StreamTopologyParserTest {

    @Test
    void parserTest() {
        var parser = new StreamTopologyParser();
        assertEquals(List.of(new StreamQuery(new Source("input_topic"),
                                             new Sink("output_topic"))),
                     parser.parse("FROM input_topic TO output_topic"));
        assertEquals(List.of(new StreamQuery(new Source("user_events",
                                                        new FieldPredicate("status", Operator.EQUAL, new StringValue("active"))),
                                             new Sink("processed_topic", "analytics_topic"))),
                     parser.parse("FROM user_events WHERE status = \"active\" TO processed_topic, analytics_topic"));
        assertEquals(List.of(new StreamQuery(new Source("transactions",
                                                        new AndPredicate(new FieldPredicate("amount", Operator.GREATER, new NumberValue("1000")),
                                                                         new FieldPredicate("currency", Operator.IN,
                                                                                            new ListValue(new StringValue("USD"), new StringValue("EUR"))))),
                                             new Sink("high_value_topic"))),
                     parser.parse("FROM transactions WHERE amount > 1000 AND currency IN (\"USD\", \"EUR\") TO high_value_topic"));
        assertEquals(List.of(new StreamQuery(new Source("sensor_data", new AndPredicate(new FieldPredicate("temperature", Operator.BETWEEN,
                                                                                                           new RangeValue(new NumberValue("20"),
                                                                                                                          new NumberValue("30"))),
                                                                                        new NotPredicate(new FieldPredicate("alert", Operator.EQUAL,
                                                                                                                            new NullValue())))),
                                             new Sink("alerts_topic"))),
                     parser.parse("FROM sensor_data WHERE temperature BETWEEN 20 AND 30 AND NOT alert IS NULL TO alerts_topic"));

        // Add the UNIQUE BY test case
        assertEquals(List.of(new StreamQuery(new Source("clickstream",
                                                        new UniqueBy(List.of("user_id", "session_id"))),
                                             new Sink("deduped_topic"))),
                     parser.parse("FROM clickstream UNIQUE BY user_id, session_id TO deduped_topic"));
    }
}
