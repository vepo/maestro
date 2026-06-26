package dev.vepo.maestro.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.vepo.maestro.crd.StreamApplication;

class StreamApplicationReconcilerTest {
    static java.util.stream.Stream<String> catalogPipelines() {
        return java.util.stream.Stream.of(
                                          """
                                          FROM input_topic
                                          |> FILTER WHERE status = 'active'
                                          |> PROJECT fields: user_id, name, email
                                          |> TO output_topic
                                          """,
                                          """
                                          FROM clickstream
                                          |> FILTER WHERE event_type = 'page_view'
                                          |> WINDOW TUMBLING SIZE 5 MINUTES
                                          |> GROUP BY user_id, page_url
                                          |> AGGREGATE count(*) AS view_count, avg(time_on_page) AS avg_time
                                          |> TO analytics_topic
                                          """,
                                          """
                                          FROM orders
                                          |> JOIN users ON orders.user_id = users.id LOOKUP TABLE 'users'
                                          |> JOIN products ON orders.product_id = products.id STREAM 'product_updates'
                                          |> PROJECT enriched_order: order_id, user_name, product_name, quantity, price
                                          |> TO enriched_orders
                                          """,
                                          """
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
                                          """,
                                          """
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
                                          """,
                                          """
                                          FROM ad_impressions
                                          |> WINDOW TUMBLING SIZE 1 HOUR
                                          |> JOIN ad_clicks ON impression_id = click_id WITHIN 30 MINUTES
                                          |> PROJECT
                                             ad_id,
                                             impression_time,
                                             click_time,
                                             time_to_click = click_time - impression_time
                                          |> TO ad_performance
                                          """,
                                          """
                                          FROM stock_ticks
                                          |> PATTERN
                                             DROP = price < stop_loss,
                                             REBOUND = price > take_profit WITHIN 5 MINUTES AFTER DROP
                                          |> DETECT AS volatility_event
                                          |> TO alerts
                                          """,
                                          """
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
                                          """);
    }

    private final StreamApplicationReconciler reconciler = new StreamApplicationReconciler();

    @ParameterizedTest
    @MethodSource("catalogPipelines")
    void shouldReconcileCatalogPipeline(String pipeline) {
        var cr = new StreamApplication();
        cr.getMetadata().setName("catalog-app");
        cr.getSpec().setPipeline(pipeline);
        cr.getSpec().getKafka().setBootstrapServers("kafka:9092");
        cr.getSpec().getKafka().setApplicationId("catalog-app");
        cr.getSpec().setImage("maestro-app:local");

        var result = reconciler.reconcile(cr);

        assertEquals("Synced", result.phase());
        assertNotNull(result.deployment());
        assertEquals("kafka:9092", result.deployment().getSpec().getTemplate().getSpec().getContainers().getFirst().getEnv().stream()
                                         .filter(e -> "KAFKA_BOOTSTRAP_SERVERS".equals(e.getName())).findFirst().orElseThrow().getValue());
    }
}
