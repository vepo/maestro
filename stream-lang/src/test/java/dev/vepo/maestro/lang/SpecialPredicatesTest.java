package dev.vepo.maestro.lang;

import static dev.vepo.maestro.lang.model.Expression.in;
import static dev.vepo.maestro.lang.model.Literal.string;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.BetweenPredicate;
import dev.vepo.maestro.lang.model.IsNotNullPredicate;
import dev.vepo.maestro.lang.model.IsNullPredicate;
import dev.vepo.maestro.lang.model.LikePredicate;
import dev.vepo.maestro.lang.model.NumberLiteral;
import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.RegexPredicate;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;

class SpecialPredicatesTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseInPredicate() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("users",
                                                                                  in("country", string("USA"),
                                                                                     string("Canada"),
                                                                                     string("Mexico")))),
                                               "na_users")),
                     parser.parse("FROM users WHERE country IN (\"USA\", \"Canada\", \"Mexico\") TO na_users"));
    }

    @Test
    void shouldParseBetweenPredicate() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("sensors",
                                                                                  new BetweenPredicate("temperature",
                                                                                                       new NumberLiteral("18.5"),
                                                                                                       new NumberLiteral("25.0")))),
                                               "comfortable")),
                     parser.parse("FROM sensors WHERE temperature BETWEEN 18.5 AND 25.0 TO comfortable"));
    }

    @Test
    void shouldParseIsNullPredicate() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("events",
                                                                                  new IsNullPredicate("error_details"))),
                                               "errors")),
                     parser.parse("FROM events WHERE error_details IS NULL TO errors"));
    }

    @Test
    void shouldParseIsNotNullPredicate() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("users",
                                                                                  new IsNotNullPredicate("email"))),
                                               "valid_users")),
                     parser.parse("FROM users WHERE email IS NOT NULL TO valid_users"));
    }

    @Test
    void shouldParseLikePredicate() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("products",
                                                                                  new LikePredicate("name", "%iphone%"))),
                                               "iphones")),
                     parser.parse("FROM products WHERE name LIKE \"%iphone%\" TO iphones"));
    }

    @Test
    void shouldParseRegexPredicate() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("logs",
                                                                                  new RegexPredicate("message", "^ERROR:.*"))),
                                               "error_logs")),
                     parser.parse("FROM logs WHERE message REGEX \"^ERROR:.*\" TO error_logs"));
    }
}
