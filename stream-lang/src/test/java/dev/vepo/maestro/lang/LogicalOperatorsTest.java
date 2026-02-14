package dev.vepo.maestro.lang;

import static dev.vepo.maestro.lang.model.Expression.and;
import static dev.vepo.maestro.lang.model.Expression.eq;
import static dev.vepo.maestro.lang.model.Expression.gt;
import static dev.vepo.maestro.lang.model.Expression.in;
import static dev.vepo.maestro.lang.model.Expression.lt;
import static dev.vepo.maestro.lang.model.Expression.not;
import static dev.vepo.maestro.lang.model.Expression.or;
import static dev.vepo.maestro.lang.model.Expression.parenthesis;
import static dev.vepo.maestro.lang.model.Literal.number;
import static dev.vepo.maestro.lang.model.Literal.string;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.FieldReferenceExpression;
import dev.vepo.maestro.lang.model.LiteralExpression;
import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;

@DisplayName("WHERE Clause - Logical Operators")
class LogicalOperatorsTest {
   private StreamTopologyParser parser = new StreamTopologyParser();

   @Test
   void shouldParseAndExpression() {

      assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("events",
                                                                                and(eq(new FieldReferenceExpression("type"),
                                                                                       new LiteralExpression(string("click"))),
                                                                                    gt(new FieldReferenceExpression("value"),
                                                                                       new LiteralExpression(number("10")))))),
                                             "clicks")),
                   parser.parse("FROM events WHERE type = \"click\" AND value > 10 TO clicks"));
   }

   @Test
   void shouldParseOrExpression() {
      assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("errors",
                                                                                or(eq(new FieldReferenceExpression("code"),
                                                                                      new LiteralExpression(number("404"))),
                                                                                   eq(new FieldReferenceExpression("code"),
                                                                                      new LiteralExpression(number("500")))))),
                                             "http_errors")),
                   parser.parse("FROM errors WHERE code = 404 OR code = 500 TO http_errors"));
   }

   @Test
   void shouldParseComplexLogicalExpression() {
      assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("transactions",
                                                                                or(parenthesis(and(gt(new FieldReferenceExpression("amount"),
                                                                                                      new LiteralExpression(number("1000"))),
                                                                                                   in("currency",
                                                                                                      string("USD"),
                                                                                                      string("EUR")))),
                                                                                   parenthesis(and(gt(new FieldReferenceExpression("amount"),
                                                                                                      new LiteralExpression(number("5000"))),
                                                                                                   in("currency", string("GBP"))))))),
                                             "high_value")),
                   parser.parse("FROM transactions WHERE (amount > 1000 AND currency IN (\"USD\", \"EUR\")) OR (amount > 5000 AND currency IN (\"GBP\")) TO high_value"));
   }

   @Test
   void shouldParseNotExpression() {
      assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("logs",
                                                                                not(eq(new FieldReferenceExpression("level"),
                                                                                       new LiteralExpression(string("DEBUG")))))),
                                             "non_debug")),
                   parser.parse("FROM logs WHERE NOT level = \"DEBUG\" TO non_debug"));
   }

   @Test
   void shouldParseParenthesizedExpression() {
      assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("data",
                                                                                parenthesis(and(gt(new FieldReferenceExpression("a"),
                                                                                                   new LiteralExpression(number("10"))),
                                                                                                lt(new FieldReferenceExpression("b"),
                                                                                                   new LiteralExpression(number("20"))))))),
                                             "filtered")),
                   parser.parse("FROM data WHERE (a > 10 AND b < 20) TO filtered"));
   }

}
