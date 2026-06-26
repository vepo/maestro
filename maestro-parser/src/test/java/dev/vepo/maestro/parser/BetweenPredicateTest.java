package dev.vepo.maestro.parser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.model.BetweenPredicate;
import dev.vepo.maestro.parser.model.FilterStage;
import dev.vepo.maestro.parser.model.NumberLiteral;

class BetweenPredicateTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseBetweenPredicateInFilter() {
        Scenario.given("a FilterStage with a BETWEEN predicate on age")
                .when("the Stream Language is parsed")
                .then("the StreamModel contains a BetweenPredicate with numeric bounds")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var model = parser.parse("""
                                                  FROM events
                                                  |> FILTER WHERE age BETWEEN 18 AND 65
                                                  |> TO active_events
                                                  """);
                         var filter = (FilterStage) model.queries().getFirst().sourcePipeline().processingStages().getFirst();
                         var between = assertInstanceOf(BetweenPredicate.class, filter.condition());
                         assertInstanceOf(NumberLiteral.class, between.lowerBound());
                         assertInstanceOf(NumberLiteral.class, between.upperBound());
                     });
    }
}
