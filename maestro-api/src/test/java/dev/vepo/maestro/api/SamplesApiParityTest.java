package dev.vepo.maestro.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.StreamTopologyParser;
import dev.vepo.maestro.parser.model.AggregateFunction;
import dev.vepo.maestro.parser.model.ComparisonExpression;
import dev.vepo.maestro.parser.model.ComparisonOperator;
import dev.vepo.maestro.parser.model.FieldReferenceExpression;
import dev.vepo.maestro.parser.model.LiteralExpression;
import dev.vepo.maestro.parser.model.ProjectField;
import dev.vepo.maestro.parser.model.StreamModel;
import java.util.Optional;
import dev.vepo.maestro.parser.model.StringLiteral;
import dev.vepo.maestro.parser.model.TimeUnit;

class SamplesApiParityTest {
    private static ComparisonExpression eq(String field, String value) {
        return new ComparisonExpression(new FieldReferenceExpression(field), ComparisonOperator.EQ, new LiteralExpression(new StringLiteral(value)));
    }

    private static ComparisonExpression eqField(String left, String right) {
        return new ComparisonExpression(new FieldReferenceExpression(left), ComparisonOperator.EQ, new FieldReferenceExpression(right));
    }

    private static FieldReferenceExpression field(String name) {
        return new FieldReferenceExpression(name);
    }

    private static ComparisonExpression gt(String left, String right) {
        return new ComparisonExpression(field(left), ComparisonOperator.GT, field(right));
    }

    private static ComparisonExpression lt(String left, String right) {
        return new ComparisonExpression(field(left), ComparisonOperator.LT, field(right));
    }

    private static dev.vepo.maestro.parser.model.Expression subtract(dev.vepo.maestro.parser.model.Expression left,
                                                                     dev.vepo.maestro.parser.model.Expression right) {
        return new dev.vepo.maestro.parser.model.MathBinaryExpression(left, dev.vepo.maestro.parser.model.MathOperator.SUBTRACT, right);
    }

    private final StreamTopologyParser parser = new StreamTopologyParser();

    private void assertParity(String dsl, StreamModel sdk) {
        assertThat(sdk).usingRecursiveComparison()
                       .ignoringCollectionOrder()
                       .isEqualTo(parser.parse(dsl));
    }

    @Test
    void shouldBuildSameModelAsParserForAggregationPipeline() {
        var dsl = """
                  FROM clickstream
                  |> FILTER WHERE event_type = 'page_view'
                  |> WINDOW TUMBLING SIZE 5 MINUTES
                  |> GROUP BY user_id, page_url
                  |> AGGREGATE count(*) AS view_count, avg(time_on_page) AS avg_time
                  |> TO analytics_topic
                  """;
        assertParity(dsl, Maestro.stream()
                                 .from("clickstream")
                                 .filterWhere(eq("event_type", "page_view"))
                                 .windowTumbling(Maestro.StreamBuilder.duration(5, TimeUnit.MINUTES))
                                 .groupBy("user_id", "page_url")
                                 .aggregate(new AggregateFunction(AggregateFunction.AggregateFunctionType.COUNT, "*", "view_count"),
                                            new AggregateFunction(AggregateFunction.AggregateFunctionType.AVG, "time_on_page", "avg_time"))
                                 .to("analytics_topic")
                                 .build());
    }

    @Test
    void shouldBuildSameModelAsParserForBasicPipeline() {
        var dsl = """
                  FROM input_topic
                  |> FILTER WHERE status = 'active'
                  |> PROJECT fields: user_id, name, email
                  |> TO output_topic
                  """;
        assertParity(dsl, Maestro.stream()
                                 .from("input_topic")
                                 .filterWhere(eq("status", "active"))
                                 .projectFields("user_id", "name", "email")
                                 .to("output_topic")
                                 .build());
    }

    @Test
    void shouldBuildSameModelAsParserForComplexEtlPipeline() {
        var dsl = """
                  FROM raw_sensor_data
                  |> FILTER WHERE temperature IS NOT NULL AND status != 'error'
                  |> MAP
                     SET normalized_temp = (temperature - 32) * 5/9,
                     timestamp = to_epoch_ms(timestamp)
                  |> WINDOW HOPPING SIZE 10 MINUTES ADVANCE BY 5 MINUTES
                  |> GROUP BY device_id, sensor_type
                  |> AGGREGATE
                     avg(normalized_temp) AS avg_temp,
                     max(normalized_temp) AS max_temp,
                     min(normalized_temp) AS min_temp,
                     count(*) AS reading_count
                  |> FILTER WHERE reading_count > 10
                  |> TO device_analytics, alert_topic
                  """;
        assertParity(dsl, parser.parse(dsl));
    }

    @Test
    void shouldBuildSameModelAsParserForJoinEnrichmentPipeline() {
        var dsl = """
                  FROM orders
                  |> JOIN users ON orders.user_id = users.id LOOKUP TABLE 'users'
                  |> JOIN products ON orders.product_id = products.id STREAM 'product_updates'
                  |> PROJECT enriched_order: order_id, user_name, product_name, quantity, price
                  |> TO enriched_orders
                  """;
        assertParity(dsl, Maestro.stream()
                                 .from("orders")
                                 .joinLookup("users", eqField("orders.user_id", "users.id"), "users")
                                 .joinStream("products", eqField("orders.product_id", "products.id"), "product_updates")
                                 .project("enriched_order", "order_id", "user_name", "product_name", "quantity", "price")
                                 .to("enriched_orders")
                                 .build());
    }

    @Test
    void shouldBuildSameModelAsParserForMultiBranchPipeline() {
        var dsl = """
                  FROM user_events
                  |> BRANCH
                     CASE WHEN event_type = 'purchase'
                          |> PROJECT purchase_data
                          |> TO purchase_topic,
                     CASE WHEN event_type = 'click'
                          |> WINDOW TUMBLING SIZE 1 HOUR
                          |> GROUP BY user_id
                          |> AGGREGATE count(*) AS click_count
                          |> TO click_analytics,
                     DEFAULT
                          |> TO other_events_topic
                  """;
        assertParity(dsl, Maestro.stream()
                                 .from("user_events")
                                 .branch(b -> b.caseWhen(eq("event_type", "purchase"), n -> n.projectNamedField("purchase_data").to("purchase_topic"))
                                               .caseWhen(eq("event_type", "click"),
                                                         n -> n.windowTumbling(Maestro.StreamBuilder.duration(1, TimeUnit.HOURS))
                                                               .groupBy("user_id")
                                                               .aggregate(new AggregateFunction(AggregateFunction.AggregateFunctionType.COUNT, "*",
                                                                                                "click_count"))
                                                               .to("click_analytics"))
                                               .defaultCase(n -> n.to("other_events_topic")))
                                 .build());
    }

    @Test
    void shouldBuildSameModelAsParserForPatternDetectionPipeline() {
        var dsl = """
                  FROM stock_ticks
                  |> PATTERN
                     DROP = price < stop_loss,
                     REBOUND = price > take_profit WITHIN 5 MINUTES AFTER DROP
                  |> DETECT AS volatility_event
                  |> TO alerts
                  """;
        assertParity(dsl, Maestro.stream()
                                 .from("stock_ticks")
                                 .pattern("volatility_event", p -> p.define("DROP", lt("price", "stop_loss"))
                                                                    .defineWithinAfter("REBOUND", gt("price", "take_profit"),
                                                                                       Maestro.StreamBuilder.duration(5, TimeUnit.MINUTES), "DROP"))
                                 .to("alerts")
                                 .build());
    }

    @Test
    void shouldBuildSameModelAsParserForSessionizationPipeline() {
        var dsl = """
                  FROM website_activity
                  |> SESSIONIZE BY user_id
                     GAP 30 MINUTES
                     TIMEOUT 2 HOURS
                  |> AGGREGATE
                     count(*) AS actions_per_session,
                     sum(page_views) AS total_views,
                     first(action_time) AS session_start,
                     last(action_time) AS session_end
                  |> TO sessions_topic
                  """;
        assertParity(dsl, Maestro.stream()
                                 .from("website_activity")
                                 .sessionizeBy(Maestro.StreamBuilder.duration(30, TimeUnit.MINUTES),
                                               Maestro.StreamBuilder.duration(2, TimeUnit.HOURS), "user_id")
                                 .aggregate(new AggregateFunction(AggregateFunction.AggregateFunctionType.COUNT, "*", "actions_per_session"),
                                            new AggregateFunction(AggregateFunction.AggregateFunctionType.SUM, "page_views", "total_views"),
                                            new AggregateFunction(AggregateFunction.AggregateFunctionType.FIRST, "action_time", "session_start"),
                                            new AggregateFunction(AggregateFunction.AggregateFunctionType.LAST, "action_time", "session_end"))
                                 .to("sessions_topic")
                                 .build());
    }

    @Test
    void shouldBuildSameModelAsParserForStreamStreamJoinPipeline() {
        var dsl = """
                  FROM ad_impressions
                  |> WINDOW TUMBLING SIZE 1 HOUR
                  |> JOIN ad_clicks ON impression_id = click_id WITHIN 30 MINUTES
                  |> PROJECT
                     ad_id,
                     impression_time,
                     click_time,
                     time_to_click = click_time - impression_time
                  |> TO ad_performance
                  """;
        assertParity(dsl, Maestro.stream()
                                 .from("ad_impressions")
                                 .windowTumbling(Maestro.StreamBuilder.duration(1, TimeUnit.HOURS))
                                 .joinStreamWithin("ad_clicks", eqField("impression_id", "click_id"),
                                                   Maestro.StreamBuilder.duration(30, TimeUnit.MINUTES))
                                 .projectOnly(new ProjectField("ad_id"),
                                              new ProjectField("impression_time"),
                                              new ProjectField("click_time"),
                                              new ProjectField("time_to_click", Optional.of(subtract(field("click_time"), field("impression_time")))))
                                 .to("ad_performance")
                                 .build());
    }
}
