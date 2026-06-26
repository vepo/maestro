package dev.vepo.maestro.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.model.AggregateStage;
import dev.vepo.maestro.parser.model.BranchStage;
import dev.vepo.maestro.parser.model.FilterStage;
import dev.vepo.maestro.parser.model.GroupByStage;
import dev.vepo.maestro.parser.model.JoinStage;
import dev.vepo.maestro.parser.model.MapStage;
import dev.vepo.maestro.parser.model.PatternStage;
import dev.vepo.maestro.parser.model.ProjectStage;
import dev.vepo.maestro.parser.model.SessionizeStage;
import dev.vepo.maestro.parser.model.WindowStage;

class SamplesParseTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseAggregationPipelineWithWindowGroupAndAggregate() {
        Scenario.given("a tumbling window aggregation Query")
                .when("the click analytics catalog pipeline is parsed")
                .then("the StreamModel contains window, group, and aggregate stages")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var stages = parser.parse("""
                                                   FROM clickstream
                                                   |> FILTER WHERE event_type = 'page_view'
                                                   |> WINDOW TUMBLING SIZE 5 MINUTES
                                                   |> GROUP BY user_id, page_url
                                                   |> AGGREGATE count(*) AS view_count, avg(time_on_page) AS avg_time
                                                   |> TO analytics_topic
                                                   """).queries().getFirst().sourcePipeline().processingStages();
                         assertThat(stages).hasSize(4);
                         assertInstanceOf(FilterStage.class, stages.get(0));
                         assertInstanceOf(WindowStage.class, stages.get(1));
                         assertInstanceOf(GroupByStage.class, stages.get(2));
                         assertInstanceOf(AggregateStage.class, stages.get(3));
                     });
    }

    @Test
    void shouldParseBasicPipelineWithFilterAndProjectStages() {
        Scenario.given("a Query with Filter and Project stages")
                .when("the basic catalog pipeline is parsed")
                .then("the StreamModel lists filter, project, and sink topics")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var query = parser.parse("""
                                                  FROM input_topic
                                                  |> FILTER WHERE status = 'active'
                                                  |> PROJECT fields: user_id, name, email
                                                  |> TO output_topic
                                                  """).queries().getFirst();
                         assertThat(query.sinkTopics()).containsExactly("output_topic");
                         assertThat(query.sourcePipeline().processingStages()).hasSize(2);
                         assertInstanceOf(FilterStage.class, query.sourcePipeline().processingStages().get(0));
                         assertInstanceOf(ProjectStage.class, query.sourcePipeline().processingStages().get(1));
                     });
    }

    @Test
    void shouldParseComplexEtlPipelineWithMapAndPostAggregateFilter() {
        Scenario.given("a complex ETL Query with map and post-aggregate filter")
                .when("the sensor ETL catalog pipeline is parsed")
                .then("the StreamModel contains map, hopping window, aggregate, and second filter")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var query = parser.parse("""
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
                                                  """).queries().getFirst();
                         assertThat(query.sinkTopics()).containsExactly("device_analytics", "alert_topic");
                         var stages = query.sourcePipeline().processingStages();
                         assertThat(stages).hasSize(6);
                         assertInstanceOf(MapStage.class, stages.get(1));
                         assertInstanceOf(FilterStage.class, stages.get(5));
                     });
    }

    @Test
    void shouldParseJoinEnrichmentPipelineWithLookupAndStreamJoins() {
        Scenario.given("a join enrichment Query")
                .when("the orders enrichment catalog pipeline is parsed")
                .then("the StreamModel contains two join stages and a project stage")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var stages = parser.parse("""
                                                   FROM orders
                                                   |> JOIN users ON orders.user_id = users.id LOOKUP TABLE 'users'
                                                   |> JOIN products ON orders.product_id = products.id STREAM 'product_updates'
                                                   |> PROJECT enriched_order: order_id, user_name, product_name, quantity, price
                                                   |> TO enriched_orders
                                                   """).queries().getFirst().sourcePipeline().processingStages();
                         assertThat(stages).hasSize(3);
                         assertInstanceOf(JoinStage.class, stages.get(0));
                         assertInstanceOf(JoinStage.class, stages.get(1));
                         assertInstanceOf(ProjectStage.class, stages.get(2));
                     });
    }

    @Test
    void shouldParseMultiBranchPipelineWithNestedStages() {
        Scenario.given("a multi-branch Query")
                .when("the branch events catalog pipeline is parsed")
                .then("the StreamModel contains a BranchStage with three cases")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var branch =
                                 assertInstanceOf(BranchStage.class, parser.parse("""
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
                                                                                  """).queries().getFirst().sourcePipeline().processingStages().getFirst());
                         assertThat(branch.cases()).hasSize(3);
                     });
    }

    @Test
    void shouldParsePatternDetectionPipelineWithDetectAlias() {
        Scenario.given("a pattern detection Query")
                .when("the stock ticks catalog pipeline is parsed")
                .then("the StreamModel contains a PatternStage with detect alias")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var pattern =
                                 assertInstanceOf(PatternStage.class, parser.parse("""
                                                                                   FROM stock_ticks
                                                                                   |> PATTERN
                                                                                      DROP = price < stop_loss,
                                                                                      REBOUND = price > take_profit WITHIN 5 MINUTES AFTER DROP
                                                                                   |> DETECT AS volatility_event
                                                                                   |> TO alerts
                                                                                   """).queries().getFirst().sourcePipeline().processingStages().getFirst());
                         assertThat(pattern.detectAlias()).isEqualTo("volatility_event");
                         assertThat(pattern.definitions()).hasSize(2);
                     });
    }

    @Test
    void shouldParseSessionizationPipelineWithAggregate() {
        Scenario.given("a sessionization Query")
                .when("the website activity catalog pipeline is parsed")
                .then("the StreamModel contains sessionize and aggregate stages")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var stages = parser.parse("""
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
                                                   """).queries().getFirst().sourcePipeline().processingStages();
                         assertThat(stages).hasSize(2);
                         assertInstanceOf(SessionizeStage.class, stages.get(0));
                         assertInstanceOf(AggregateStage.class, stages.get(1));
                     });
    }

    @Test
    void shouldParseStreamStreamJoinPipelineWithWithinClause() {
        Scenario.given("a stream-stream join Query")
                .when("the ad performance catalog pipeline is parsed")
                .then("the StreamModel contains window, join-within, and project stages")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var stages = parser.parse("""
                                                   FROM ad_impressions
                                                   |> WINDOW TUMBLING SIZE 1 HOUR
                                                   |> JOIN ad_clicks ON impression_id = click_id WITHIN 30 MINUTES
                                                   |> PROJECT
                                                      ad_id,
                                                      impression_time,
                                                      click_time,
                                                      time_to_click = click_time - impression_time
                                                   |> TO ad_performance
                                                   """).queries().getFirst().sourcePipeline().processingStages();
                         assertThat(stages).hasSize(3);
                         assertInstanceOf(WindowStage.class, stages.get(0));
                         var join = assertInstanceOf(JoinStage.class, stages.get(1));
                         assertThat(join.within()).isPresent();
                     });
    }
}
