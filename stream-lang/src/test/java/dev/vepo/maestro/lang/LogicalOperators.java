package dev.vepo.maestro.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.ComparisonExpression;
import dev.vepo.maestro.lang.model.ComparisonOperator;
import dev.vepo.maestro.lang.model.FieldReferenceExpression;
import dev.vepo.maestro.lang.model.LiteralExpression;
import dev.vepo.maestro.lang.model.LogicalExpression;
import dev.vepo.maestro.lang.model.LogicalOperator;
import dev.vepo.maestro.lang.model.NumberLiteral;
import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;
import dev.vepo.maestro.lang.model.StringLiteral;

@DisplayName("WHERE Clause - Logical Operators")
class LogicalOperators {
    @Test
    void shouldParseAndExpression() {
        var parser = new StreamTopologyParser();
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("events",
                                                                                  new LogicalExpression(new ComparisonExpression(new FieldReferenceExpression("type"),
                                                                                                                                 ComparisonOperator.EQ,
                                                                                                                                 new LiteralExpression(new StringLiteral("click"))),
                                                                                                        LogicalOperator.AND,
                                                                                                        new ComparisonExpression(
                                                                                                                                 new FieldReferenceExpression("value"),
                                                                                                                                 ComparisonOperator.GT,
                                                                                                                                 new LiteralExpression(new NumberLiteral("10")))))),
                                               "clicks")),
                     parser.parse("FROM events WHERE type = \"click\" AND value > 10 TO clicks"));
    }
}
