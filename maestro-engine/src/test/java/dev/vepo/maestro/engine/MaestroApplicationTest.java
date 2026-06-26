package dev.vepo.maestro.engine;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
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
    private Set<String> testTopics;

    private MaestroApplication app(String pipeline) {
        return new MaestroApplication(pipeline, new MaestroConfigs(Map.of(
                                                                          BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers(),
                                                                          APPLICATION_ID_CONFIG, "test-" + UUID.randomUUID())));
    }

    @BeforeEach
    void createTopics() throws InterruptedException, ExecutionException, TimeoutException {
        adminClient = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers()));
        testTopics = Set.of("input", "output");
        List<NewTopic> topics = testTopics.stream()
                                          .map(name -> new NewTopic(name, 1, (short) 1))
                                          .collect(Collectors.toList());
        adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> assertThat(adminClient.listTopics().names().get(5, TimeUnit.SECONDS)).containsAll(testTopics));
    }

    @AfterEach
    void deleteTopics() throws InterruptedException, ExecutionException, TimeoutException {
        if (adminClient != null) {
            try {
                var existingTopics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
                var topicsToDelete = existingTopics.stream()
                                                   .filter(topic -> testTopics.contains(topic) || topic.startsWith("_test_"))
                                                   .collect(Collectors.toSet());
                if (!topicsToDelete.isEmpty()) {
                    adminClient.deleteTopics(topicsToDelete).all().get(30, TimeUnit.SECONDS);
                }
            } finally {
                adminClient.close();
            }
        }
    }

    private SendDSL send(String topic) {
        return new SendDSL(topic, broker.getBootstrapServers());
    }

    @Test
    void shouldFilterRecordsByPredicate() {
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
    void shouldPassThroughRecordsFromSourceToSink() {
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
    void shouldProjectSelectedFields() {
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
    void shouldRejectUnsupportedBranchStageAtRuntime() {
        assertThatThrownBy(() -> {
            try (var app = app("""
                               FROM user_events
                               |> BRANCH
                                  CASE WHEN event_type = 'purchase'
                                       |> TO purchase_topic,
                                  DEFAULT
                                       |> TO other_events_topic
                               """)) {
                app.start();
            }
        }).isInstanceOf(UnsupportedStageException.class);
    }

    @Test
    void shouldRejectUnsupportedPatternStageAtRuntime() {
        assertThatThrownBy(() -> {
            try (var app = app("""
                               FROM stock_ticks
                               |> PATTERN
                                  DROP = price < stop_loss
                               |> DETECT AS volatility_event
                               |> TO alerts
                               """)) {
                app.start();
            }
        }).isInstanceOf(UnsupportedStageException.class);
    }

    @Test
    void shouldRejectUnsupportedSessionizeStageAtRuntime() {
        assertThatThrownBy(() -> {
            try (var app = app("""
                               FROM website_activity
                               |> SESSIONIZE BY user_id
                                  GAP 30 MINUTES
                               |> TO sessions_topic
                               """)) {
                app.start();
            }
        }).isInstanceOf(UnsupportedStageException.class);
    }

    private VerifyDSL verify(String topic) {
        return new VerifyDSL(topic, broker.getBootstrapServers());
    }
}
