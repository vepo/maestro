package dev.vepo.maestro.engine;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import dev.vepo.maestro.engine.MaestroApplication.State;

@Testcontainers
class MaestroApplicationTest {
    @Container
    private final KafkaContainer broker = new KafkaContainer("apache/kafka-native:4.0.0");

    private AdminClient adminClient;
    private final Set<String> createdTopics = new HashSet<>();

    private MaestroApplication app(String pipeline) {
        return new MaestroApplication(pipeline, new MaestroConfigs(Map.of(
                                                                          BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers(),
                                                                          APPLICATION_ID_CONFIG, "test-" + UUID.randomUUID())));
    }

    private void createTopics(String... names) throws InterruptedException, ExecutionException, TimeoutException {
        createdTopics.addAll(Arrays.asList(names));
        var topics = Arrays.stream(names).map(name -> new NewTopic(name, 1, (short) 1)).collect(Collectors.toList());
        adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> assertThat(adminClient.listTopics().names().get(5, TimeUnit.SECONDS)).containsAll(createdTopics));
    }

    private SendDSL send(String topic) {
        return new SendDSL(topic, broker.getBootstrapServers());
    }

    @BeforeEach
    void setUp() {
        adminClient = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers()));
        createdTopics.clear();
    }

    @Test
    void shouldAggregateWithTumblingWindow() throws Exception {
        createTopics("clickstream", "analytics_topic");
        try (var app = app("""
                           FROM clickstream
                           |> FILTER WHERE event_type = 'page_view'
                           |> WINDOW TUMBLING SIZE 3 SECONDS
                           |> GROUP BY user_id, page_url
                           |> AGGREGATE count(*) AS view_count
                           |> TO analytics_topic
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("clickstream")
                               .key("u1").json("{\"event_type\": \"page_view\", \"user_id\": \"u1\", \"page_url\": \"/home\"}")
                               .key("u1").json("{\"event_type\": \"page_view\", \"user_id\": \"u1\", \"page_url\": \"/home\"}")
                               .records();

            Thread.sleep(8_000);

            verify("analytics_topic")
                                     .within(30, TimeUnit.SECONDS)
                                     .received(1)
                                     .records()
                                     .assertFirst(record -> assertThat(record.value()).contains("view_count"));
        }
    }

    @Test
    void shouldDetectPatternSequence() throws Exception {
        createTopics("stock_ticks", "alerts");
        try (var app = app("""
                           FROM stock_ticks
                           |> PATTERN
                              DROP = price < stop_loss,
                              REBOUND = price > take_profit WITHIN 5 MINUTES AFTER DROP
                           |> DETECT AS volatility_event
                           |> TO alerts
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("stock_ticks")
                               .key("AAPL").json("{\"price\": 90, \"stop_loss\": 100, \"take_profit\": 95}")
                               .key("AAPL").json("{\"price\": 96, \"stop_loss\": 100, \"take_profit\": 95}")
                               .records();

            verify("alerts")
                            .within(15, TimeUnit.SECONDS)
                            .received(1)
                            .records()
                            .assertFirst(record -> assertThat(record.value()).contains("volatility_event"));
        }
    }

    @Test
    void shouldFilterAfterAggregate() throws Exception {
        createTopics("raw_sensor_data", "device_analytics", "alert_topic");
        try (var app = app("""
                           FROM raw_sensor_data
                           |> WINDOW TUMBLING SIZE 3 SECONDS
                           |> GROUP BY device_id
                           |> AGGREGATE count(*) AS reading_count
                           |> FILTER WHERE reading_count > 1
                           |> TO device_analytics
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("raw_sensor_data")
                                   .json("{\"device_id\": \"d1\"}")
                                   .json("{\"device_id\": \"d1\"}")
                                   .json("{\"device_id\": \"d1\"}")
                                   .records();

            Thread.sleep(8_000);

            verify("device_analytics")
                                      .within(30, TimeUnit.SECONDS)
                                      .received(1)
                                      .records()
                                      .assertFirst(record -> assertThat(record.value()).contains("reading_count"));
        }
    }

    @Test
    void shouldFilterRecordsByPredicate() throws Exception {
        createTopics("input", "output");
        try (var app = app("""
                           FROM input
                           |> FILTER WHERE status = 'active'
                           |> TO output
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("input")
                         .key("user-1").json("{\"id\": 1, \"status\": \"active\"}")
                         .key("user-2").json("{\"id\": 2, \"status\": \"inactive\"}")
                         .key("user-3").json("{\"id\": 3, \"status\": \"active\"}")
                         .records();

            verify("output")
                            .within(15, TimeUnit.SECONDS)
                            .received(2)
                            .records()
                            .assertThat(records -> assertThat(records).extracting(r -> r.key()).containsExactlyInAnyOrder("user-1", "user-3"));
        }
    }

    @Test
    void shouldJoinLookupTable() throws Exception {
        createTopics("orders", "users", "enriched_orders");
        try (var usersApp = app("""
                                FROM users
                                |> TO users
                                """)) {
            usersApp.start();
            await().until(() -> usersApp.state() == State.RUNNING);
            send("users").key("u1").json("{\"id\": \"u1\", \"user_name\": \"Alice\"}").records();
            Thread.sleep(2_000);
        }

        try (var app = app("""
                           FROM orders
                           |> JOIN users ON user_id = id LOOKUP TABLE 'users'
                           |> TO enriched_orders
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("orders").key("o1").json("{\"user_id\": \"u1\", \"order_id\": 1}").records();

            verify("enriched_orders")
                                     .within(20, TimeUnit.SECONDS)
                                     .received(1)
                                     .records()
                                     .assertFirst(record -> assertThat(record.value()).contains("users"));
        }
    }

    @Test
    void shouldJoinStreamsWithinWindow() throws Exception {
        createTopics("ad_impressions", "ad_clicks", "ad_performance");
        try (var app = app("""
                           FROM ad_impressions
                           |> JOIN ad_clicks ON impression_id = click_id WITHIN 30 SECONDS
                           |> TO ad_performance
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("ad_clicks").key("imp1").json("{\"click_id\": \"imp1\", \"click_time\": 2}").records();
            send("ad_impressions").key("imp1").json("{\"impression_id\": \"imp1\", \"impression_time\": 1}").records();

            verify("ad_performance")
                                    .within(20, TimeUnit.SECONDS)
                                    .received(1)
                                    .records()
                                    .assertFirst(record -> assertThat(record.value()).contains("ad_clicks"));
        }
    }

    @Test
    void shouldMapAndTransformFields() throws Exception {
        createTopics("raw_sensor_data", "device_analytics");
        try (var app = app("""
                           FROM raw_sensor_data
                           |> MAP SET normalized_temp = (temperature - 32) * 5/9
                           |> TO device_analytics
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("raw_sensor_data").json("{\"temperature\": 32}").records();

            verify("device_analytics")
                                      .within(15, TimeUnit.SECONDS)
                                      .received(1)
                                      .records()
                                      .assertFirst(record -> assertThat(record.value()).contains("normalized_temp"));
        }
    }

    @Test
    void shouldPassThroughRecordsFromSourceToSink() throws Exception {
        createTopics("input", "output");
        try (var app = app("""
                           FROM input
                           |> TO output
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("input")
                         .json("{\"id\": 1, \"name\": \"John Doe\", \"status\": \"active\"}")
                         .json("{\"id\": 2, \"name\": \"Jane Smith\", \"status\": \"inactive\"}")
                         .records();

            verify("output")
                            .within(15, TimeUnit.SECONDS)
                            .received(2)
                            .records()
                            .assertThat(records -> assertThat(records).hasSize(2));
        }
    }

    @Test
    void shouldProjectSelectedFields() throws Exception {
        createTopics("input", "output");
        try (var app = app("""
                           FROM input
                           |> PROJECT fields: user_id, name, email
                           |> TO output
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("input")
                         .json("{\"user_id\": 101, \"name\": \"Alice\", \"email\": \"alice@example.com\", \"age\": 28}")
                         .records();

            verify("output")
                            .within(15, TimeUnit.SECONDS)
                            .received(1)
                            .records()
                            .assertFirst(record -> assertThat(record.value())
                                                                             .contains("user_id", "name", "email")
                                                                             .doesNotContain("age"));
        }
    }

    @Test
    void shouldRouteBranchEventsToDifferentSinks() throws Exception {
        createTopics("user_events", "purchase_topic", "other_events_topic");
        try (var app = app("""
                           FROM user_events
                           |> BRANCH
                              CASE WHEN event_type = 'purchase'
                                   |> TO purchase_topic,
                              DEFAULT
                                   |> TO other_events_topic
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("user_events")
                               .json("{\"event_type\": \"purchase\"}")
                               .json("{\"event_type\": \"view\"}")
                               .records();

            verify("purchase_topic").within(15, TimeUnit.SECONDS).received(1).records();
            verify("other_events_topic").within(15, TimeUnit.SECONDS).received(1).records();
        }
    }

    @Test
    void shouldSessionizeAndAggregate() throws Exception {
        createTopics("website_activity", "sessions_topic");
        try (var app = app("""
                           FROM website_activity
                           |> SESSIONIZE BY user_id
                              GAP 2 SECONDS
                           |> AGGREGATE count(*) AS actions_per_session
                           |> TO sessions_topic
                           """)) {
            app.start();
            await().until(() -> app.state() == State.RUNNING);

            send("website_activity")
                                    .key("u1").json("{\"user_id\": \"u1\"}")
                                    .key("u1").json("{\"user_id\": \"u1\"}")
                                    .records();

            Thread.sleep(6_000);

            verify("sessions_topic")
                                    .within(30, TimeUnit.SECONDS)
                                    .received(1)
                                    .records()
                                    .assertFirst(record -> assertThat(record.value()).contains("actions_per_session"));
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        if (adminClient != null) {
            try {
                if (!createdTopics.isEmpty()) {
                    adminClient.deleteTopics(createdTopics).all().get(30, TimeUnit.SECONDS);
                }
            } finally {
                adminClient.close();
            }
        }
    }

    private VerifyDSL verify(String topic) {
        return new VerifyDSL(topic, broker.getBootstrapServers());
    }
}
