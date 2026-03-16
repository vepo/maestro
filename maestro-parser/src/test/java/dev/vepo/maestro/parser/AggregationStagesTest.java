package dev.vepo.maestro.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.model.AggregateFunction;
import dev.vepo.maestro.parser.model.AggregateStage;
import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;

class AggregationStagesTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseCountAggregate() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("events"),
                                                                  new AggregateStage(List.of("event_type"),
                                                                                     new AggregateFunction(AggregateFunction.AggregateFunctionType.COUNT,
                                                                                                           "*",
                                                                                                           Optional.of("event_count")))),
                                               "counts")),
                     parser.parse("""
                                  FROM events 
                                  |> AGGREGATE BY event_type COUNT(*) AS event_count 
                                  TO counts
                                  """));
    }

    @Test
    void shouldParseMultipleAggregateFunctions() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("sales"),
                                                                  List.of(new AggregateStage(List.of("product_id", "region"),
                                                                                             new AggregateFunction(AggregateFunction.AggregateFunctionType.SUM,
                                                                                                                   "amount",
                                                                                                                   "total_sales"),
                                                                                             new AggregateFunction(AggregateFunction.AggregateFunctionType.AVG,
                                                                                                                   "amount",
                                                                                                                   "avg_sale"),
                                                                                             new AggregateFunction(AggregateFunction.AggregateFunctionType.COUNT,
                                                                                                                   "*",
                                                                                                                   "transaction_count"),
                                                                                             new AggregateFunction(AggregateFunction.AggregateFunctionType.MAX,
                                                                                                                   "amount",
                                                                                                                   Optional.of("max_sale")),
                                                                                             new AggregateFunction(AggregateFunction.AggregateFunctionType.MIN,
                                                                                                                   "amount",
                                                                                                                   "min_sale")))),
                                               "sales_summary")),
                     parser.parse("""
                                  FROM sales
                                  |> AGGREGATE BY product_id, region SUM(amount) AS total_sales, 
                                                                     AVG(amount) AS avg_sale, 
                                                                     COUNT(*) AS transaction_count, 
                                                                     MAX(amount) AS max_sale, 
                                                                     MIN(amount) AS min_sale 
                                  TO sales_summary
                                  """));
    }

    @Test
    void shouldParseFirstLastAggregateFunctions() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("user_sessions"),
                                                                  List.of(new AggregateStage(
                                                                                             List.of("user_id"),
                                                                                             new AggregateFunction(AggregateFunction.AggregateFunctionType.FIRST,
                                                                                                                   "event_time",
                                                                                                                   "session_start"),
                                                                                             new AggregateFunction(AggregateFunction.AggregateFunctionType.LAST,
                                                                                                                   "event_time",
                                                                                                                   "session_end")))),
                                               "sessions")),
                     parser.parse("""
                                  FROM user_sessions 
                                  |> AGGREGATE BY user_id FIRST(event_time) AS session_start, 
                                                          LAST(event_time) AS session_end 
                                  TO sessions
                                  """));
    }
}
