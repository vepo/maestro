package dev.vepo.maestro.engine;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

    @BeforeEach
    void createTopics() throws InterruptedException, ExecutionException, TimeoutException {
        // Initialize AdminClient
        adminClient = AdminClient.create(Map.of(
                                                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers()));

        testTopics = Set.of("input", "output");

        // Create topics with configurations
        List<NewTopic> topics = testTopics.stream()
                                          .map(name -> new NewTopic(name, 1, (short) 1) // 1 partition, replication factor 1
                                                                                       .configs(Map.of(
                                                                                                       "cleanup.policy", "delete",
                                                                                                       "retention.ms", "3600000", // 1 hour retention for tests
                                                                                                       "delete.retention.ms", "1000")))
                                          .collect(Collectors.toList());

        // Create all topics
        adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

        // Verify topics were created
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   var listedTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
                   assertThat(listedTopics).containsAll(testTopics);
               });

        System.out.println("Created topics: " + testTopics);
    }

    @AfterEach
    void deleteTopics() throws InterruptedException, ExecutionException, TimeoutException {
        if (adminClient != null) {
            try {
                // Get all topics that start with test prefix or are in our test set
                var existingTopics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
                var topicsToDelete = existingTopics.stream()
                                                   .filter(topic -> testTopics.contains(topic) || topic.startsWith("_test_"))
                                                   .collect(Collectors.toSet());

                if (!topicsToDelete.isEmpty()) {
                    // Delete topics
                    adminClient.deleteTopics(topicsToDelete).all().get(30, TimeUnit.SECONDS);

                    // Wait for deletion to complete
                    await().atMost(10, TimeUnit.SECONDS)
                           .pollInterval(500, TimeUnit.MILLISECONDS)
                           .untilAsserted(() -> {
                               var remainingTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
                               assertThat(remainingTopics).doesNotContainAnyElementsOf(topicsToDelete);
                           });

                    System.out.println("Deleted topics: " + topicsToDelete);
                }
            } finally {
                adminClient.close();
            }
        }
    }

    @Test
    void passthroughTest() {
        try (var app = new MaestroApplication("""
                                              FROM input
                                              TO output
                                              """, new MaestroConfigs(Map.of(BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers(),
                                                                             APPLICATION_ID_CONFIG, "test")))) {

            app.start();

            await().until(() -> app.state() == State.RUNNING);

            // Send test data with DSL
            send("input")
                         .json("""
                               {"id": 1, "name": "John Doe", "status": "active"}
                               """)
                         .json("""
                               {"id": 2, "name": "Jane Smith", "status": "inactive"}
                               """)
                         .json("""
                               {"id": 3, "name": "Bob Johnson", "status": "active"}
                               """)
                         .records();

            // Verify output with DSL
            verify("output")
                            .within(5, TimeUnit.SECONDS)
                            .received(3)
                            .records()
                            .assertThat(records -> {
                                assertThat(records)
                                                   .hasSize(3)
                                                   .extracting(r -> r.value())
                                                   .allSatisfy(value -> {
                                                       assertThat(value).contains("id", "name", "status");
                                                   });

                                // Check specific record content
                                assertThat(records.get(0).value())
                                                                  .contains("\"id\":1")
                                                                  .contains("\"status\":\"active\"");
                            });
        }
    }

    @Disabled
    @Test
    void filterTest() {
        try (var app = new MaestroApplication("""
                                              FROM input
                                              |> FILTER WHERE status = 'active'
                                              TO output
                                              """, new MaestroConfigs(Map.of(BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers())))) {

            app.start();

            // Send test data
            send("input")
                         .key("user-1").json("""
                                             {"id": 1, "name": "John", "status": "active"}
                                             """)
                         .key("user-2").json("""
                                             {"id": 2, "name": "Jane", "status": "inactive"}
                                             """)
                         .key("user-3").json("""
                                             {"id": 3, "name": "Bob", "status": "active"}
                                             """)
                         .records();

            // Verify only active users passed through
            verify("output")
                            .withTimeout(5, TimeUnit.SECONDS)
                            .received(2)
                            .records()
                            .assertThat(records -> {
                                assertThat(records)
                                                   .extracting(r -> r.key())
                                                   .containsExactlyInAnyOrder("user-1", "user-3");
                            });
        }
    }

    @Disabled
    @Test
    void projectionTest() {
        try (var app = new MaestroApplication("""
                                              FROM input
                                              |> PROJECT fields: user_id, name, email
                                              TO output
                                              """, new MaestroConfigs(Map.of(BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers())))) {

            app.start();

            // Send test data
            send("input")
                         .json("""
                               {
                                   "user_id": 101,
                                   "name": "Alice Wonderland",
                                   "email": "alice@example.com",
                                   "age": 28,
                                   "address": "123 Wonderland St"
                               }
                               """)
                         .records();

            // Verify only projected fields exist
            verify("output")
                            .within(Duration.ofSeconds(5))
                            .received(1)
                            .records()
                            .assertFirst(record -> {
                                assertThat(record.value())
                                                          .contains("user_id", "name", "email")
                                                          .doesNotContain("age", "address");
                            });
        }
    }

    @Disabled
    @Test
    void aggregationTest() {
        try (var app = new MaestroApplication("""
                                              FROM input
                                              |> WINDOW TUMBLING SIZE 1 MINUTE
                                              |> GROUP BY status
                                              |> AGGREGATE count(*) AS count
                                              TO output
                                              """, new MaestroConfigs(Map.of(BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers())))) {

            app.start();

            // Send multiple records within window
            send("input")
                         .batch(batch -> {
                             for (int i = 0; i < 5; i++) {
                                 batch.json("""
                                            {"id": %d, "status": "active"}
                                            """.formatted(i));
                             }
                             for (int i = 5; i < 8; i++) {
                                 batch.json("""
                                            {"id": %d, "status": "inactive"}
                                            """.formatted(i));
                             }
                         })
                         .records();

            // Wait for window to trigger
            await().atMost(10, TimeUnit.SECONDS)
                   .until(() -> app.metrics().windowTriggers() > 0);

            // Verify aggregation results
            verify("output")
                            .withPollInterval(100, TimeUnit.MILLISECONDS)
                            .within(10, TimeUnit.SECONDS)
                            .received(2)
                            .records()
                            .assertThat(records -> {
                                var activeAgg = findAggregate(records, "active");
                                var inactiveAgg = findAggregate(records, "inactive");

                                assertThat(activeAgg).contains("\"count\":5");
                                assertThat(inactiveAgg).contains("\"count\":3");
                            });
        }
    }

    @Disabled
    @Test
    void joinTest() {
        try (var app = new MaestroApplication("""
                                              FROM orders
                                              |> JOIN users ON orders.user_id = users.id LOOKUP TABLE 'users'
                                              |> PROJECT order_id, user_name, amount
                                              TO output
                                              """, new MaestroConfigs(Map.of(BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers())))) {

            app.start();

            // Setup lookup table
            app.table("users")
               .withKey("1").value("""
                                   {"id": 1, "name": "John Doe", "email": "john@example.com"}
                                   """)
               .withKey("2").value("""
                                   {"id": 2, "name": "Jane Smith", "email": "jane@example.com"}
                                   """)
               .create();

            // Send order stream
            send("orders")
                          .json("""
                                {"order_id": "ORD-001", "user_id": 1, "amount": 100.50}
                                """)
                          .json("""
                                {"order_id": "ORD-002", "user_id": 2, "amount": 75.25}
                                """)
                          .json("""
                                {"order_id": "ORD-003", "user_id": 999, "amount": 200.00}
                                """) // No matching user
                          .records();

            // Verify joined results
            verify("output")
                            .within(5, TimeUnit.SECONDS)
                            .received(2)
                            .records()
                            .assertThat(records -> {
                                assertThat(records.get(0).value())
                                                                  .contains("ORD-001", "John Doe", "100.50");
                                assertThat(records.get(1).value())
                                                                  .contains("ORD-002", "Jane Smith", "75.25");
                            });
        }
    }

    @Disabled
    @Test
    void branchTest() {
        try (var app = new MaestroApplication("""
                                              FROM events
                                              |> BRANCH
                                                 CASE WHEN type = 'click'
                                                      |> PROJECT click_data
                                                      |> TO clicks,
                                                 CASE WHEN type = 'purchase'
                                                      |> PROJECT purchase_data
                                                      |> TO purchases,
                                                 DEFAULT
                                                      |> TO other
                                              """, new MaestroConfigs(Map.of(BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers())))) {

            app.start();

            // Send mixed events
            send("events")
                          .json("""
                                {"id": 1, "type": "click", "element": "button", "page": "/home"}
                                """)
                          .json("""
                                {"id": 2, "type": "purchase", "item": "laptop", "price": 999.99}
                                """)
                          .json("""
                                {"id": 3, "type": "scroll", "position": 250, "page": "/about"}
                                """)
                          .json("""
                                {"id": 4, "type": "click", "element": "link", "page": "/products"}
                                """)
                          .records();

            // Verify each branch
            verify("clicks")
                            .within(5, TimeUnit.SECONDS)
                            .received(2)
                            .records()
                            .assertThat(records -> {
                                assertThat(records)
                                                   .extracting(r -> r.value())
                                                   .allMatch(value -> value.contains("click_data"));
                            });

            verify("purchases")
                               .received(1)
                               .records()
                               .assertFirst(record -> assertThat(record.value()).contains("purchase_data"));

            verify("other")
                           .received(1)
                           .records()
                           .assertFirst(record -> assertThat(record.value()).contains("scroll"));
        }
    }

    @Disabled
    @Test
    void errorHandlingTest() {
        try (var app = new MaestroApplication("""
                                              FROM input
                                              |> FILTER WHERE status != 'error'
                                              |> MAP
                                                   SET processed = true,
                                                   timestamp = now()
                                              TO output
                                              """, new MaestroConfigs(Map.of(BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers())))) {

            app.start();

            // Send records including invalid ones
            send("input")
                         .key("good-1").json("""
                                             {"id": 1, "status": "active"}
                                             """)
                         .key("error-1").json("""
                                              {"id": 2, "status": "error"}
                                              """)
                         .key("good-2").json("""
                                             {"id": 3, "status": "pending"}
                                             """)
                         .records();

            // Verify error topic or metrics
            verify("output")
                            .within(5, TimeUnit.SECONDS)
                            .received(2)
                            .records()
                            .assertThat(records -> {
                                assertThat(records)
                                                   .extracting(r -> r.key())
                                                   .containsExactly("good-1", "good-2");
                            });

            // Check error metrics
            assertThat(app.metrics().filteredRecords())
                                                       .isEqualTo(1);
        }
    }

    // Test Helper DSL Classes

    private SendDSL send(String topic) {
        return new SendDSL(topic, broker.getBootstrapServers());
    }

    private VerifyDSL verify(String topic) {
        return new VerifyDSL(topic, broker.getBootstrapServers());
    }

    private String findAggregate(List<ConsumerRecord<String, String>> records, String status) {
        return records.stream()
                      .map(ConsumerRecord::value)
                      .filter(value -> value.contains("\"status\":\"" + status + "\""))
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("No aggregate found for status: " + status));
    }
}