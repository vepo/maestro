// StreamTopologyParserTest.java
package dev.vepo.maestro.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.BetweenPredicate;
import dev.vepo.maestro.lang.model.ComparisonExpression;
import dev.vepo.maestro.lang.model.ComparisonOperator;
import dev.vepo.maestro.lang.model.FieldReferenceExpression;
import dev.vepo.maestro.lang.model.InPredicate;
import dev.vepo.maestro.lang.model.IsNullPredicate;
import dev.vepo.maestro.lang.model.LiteralExpression;
import dev.vepo.maestro.lang.model.LogicalExpression;
import dev.vepo.maestro.lang.model.LogicalOperator;
import dev.vepo.maestro.lang.model.NotExpression;
import dev.vepo.maestro.lang.model.NumberLiteral;
import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;
import dev.vepo.maestro.lang.model.StringLiteral;
import dev.vepo.maestro.lang.model.UniqueBy;

class StreamTopologyParserTest {

    @Test
    void parserTest() {
        var parser = new StreamTopologyParser();

        // Test 1: Simple FROM/TO query
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("input_topic")),
                                               "output_topic")),
                     parser.parse("FROM input_topic TO output_topic"));
        // Test 2: Query with WHERE clause
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("user_events",
                                                                                  new ComparisonExpression(new FieldReferenceExpression("status"),
                                                                                                           ComparisonOperator.EQ,
                                                                                                           new LiteralExpression(new StringLiteral("active")))),
                                                                  List.of()),
                                               "processed_topic", "analytics_topic")),
                     parser.parse("FROM user_events WHERE status = \"active\" TO processed_topic, analytics_topic"));
        // Test 3: Query with complex AND condition and IN operator
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("transactions",
                                                                                  new LogicalExpression(new ComparisonExpression(new FieldReferenceExpression("amount"),
                                                                                                                                 ComparisonOperator.GT,
                                                                                                                                 new LiteralExpression(new NumberLiteral("1000"))),
                                                                                                        LogicalOperator.AND,
                                                                                                        new InPredicate("currency", List.of(
                                                                                                                                            new StringLiteral("USD"),
                                                                                                                                            new StringLiteral("EUR")))))),
                                               "high_value_topic")),
                     parser.parse("FROM transactions WHERE amount > 1000 AND currency IN (\"USD\", \"EUR\") TO high_value_topic"));
        // Test 4: Query with BETWEEN and NOT IS NULL
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("sensor_data",
                                                                                  new LogicalExpression(new BetweenPredicate("temperature",
                                                                                                                             new NumberLiteral("20"),
                                                                                                                             new NumberLiteral("30")),
                                                                                                        LogicalOperator.AND,
                                                                                                        new NotExpression(new IsNullPredicate("alert"))))),
                                               "alerts_topic")),
                     parser.parse("FROM sensor_data WHERE temperature BETWEEN 20 AND 30 AND NOT alert IS NULL TO alerts_topic"));

        // Test 5: Query with UNIQUE BY
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("clickstream",
                                                                                  new UniqueBy("user_id", "session_id"))),
                                               "deduped_topic")),
                     parser.parse("FROM clickstream UNIQUE BY user_id, session_id TO deduped_topic"));
    }
}