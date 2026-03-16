package dev.vepo.maestro.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.model.ComparisonExpression;
import dev.vepo.maestro.parser.model.ComparisonOperator;
import dev.vepo.maestro.parser.model.FieldReferenceExpression;
import dev.vepo.maestro.parser.model.LiteralExpression;
import dev.vepo.maestro.parser.model.LogicalExpression;
import dev.vepo.maestro.parser.model.LogicalOperator;
import dev.vepo.maestro.parser.model.NumberLiteral;
import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;
import dev.vepo.maestro.parser.model.StringLiteral;

class WhereClauseComparisonsTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseEqualsComparison() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("users", new ComparisonExpression(new FieldReferenceExpression("status"),
                                                                                                                    ComparisonOperator.EQ,
                                                                                                                    new LiteralExpression(new StringLiteral("active"))))),
                                               "output")),
                     parser.parse("FROM users WHERE status = \"active\" TO output"));
    }

    @Test
    void shouldParseNotEqualsComparison() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("orders",
                                                                                  new ComparisonExpression(new FieldReferenceExpression("status"),
                                                                                                           ComparisonOperator.NEQ,
                                                                                                           new LiteralExpression(new StringLiteral("cancelled"))))),
                                               "valid_orders")),
                     parser.parse("FROM orders WHERE status != \"cancelled\" TO valid_orders"));
    }

    @Test
    void shouldParseAllComparisonOperators() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("metrics",
                                                                                  new LogicalExpression(new LogicalExpression(new ComparisonExpression(new FieldReferenceExpression("value"),
                                                                                                                                                       ComparisonOperator.GT,
                                                                                                                                                       new LiteralExpression(new NumberLiteral("10"))),
                                                                                                                              LogicalOperator.AND,
                                                                                                                              new ComparisonExpression(new FieldReferenceExpression("value"),
                                                                                                                                                       ComparisonOperator.LT,
                                                                                                                                                       new LiteralExpression(new NumberLiteral("100")))),
                                                                                                        LogicalOperator.AND,
                                                                                                        new ComparisonExpression(new FieldReferenceExpression("count"),
                                                                                                                                 ComparisonOperator.GTE,
                                                                                                                                 new LiteralExpression(new NumberLiteral("0")))))),
                                               "filtered_metrics")),
                     parser.parse("FROM metrics WHERE value > 10 AND value < 100 AND count >= 0 TO filtered_metrics"));
    }
}
