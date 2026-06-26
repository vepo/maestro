package dev.vepo.maestro.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.crd.StreamApplication;

class StreamApplicationReconcilerTest {
    private final StreamApplicationReconciler reconciler = new StreamApplicationReconciler();

    @Test
    void shouldFailWhenPipelineContainsBranchStage() {
        var cr = new StreamApplication();
        cr.getMetadata().setName("branch-app");
        cr.getSpec().setPipeline("""
                                 FROM user_events
                                 |> BRANCH
                                    DEFAULT
                                         |> TO other_events_topic
                                 """);
        cr.getSpec().getKafka().setBootstrapServers("kafka:9092");
        cr.getSpec().getKafka().setApplicationId("branch-app");

        var result = reconciler.reconcile(cr);

        assertEquals("Failed", result.phase());
    }

    @Test
    void shouldReconcileBasicStreamApplication() {
        var cr = new StreamApplication();
        cr.getMetadata().setName("orders-enrichment");
        cr.getSpec().setPipeline("""
                                 FROM orders
                                 |> FILTER WHERE status = 'active'
                                 |> TO enriched_orders
                                 """);
        cr.getSpec().getKafka().setBootstrapServers("kafka:9092");
        cr.getSpec().getKafka().setApplicationId("orders-enrichment");

        var result = reconciler.reconcile(cr);

        assertEquals("Synced", result.phase());
        assertNotNull(result.deployment());
        assertEquals("orders-enrichment", result.deployment().getMetadata().getName());
        assertEquals("kafka:9092", result.deployment().getSpec().getTemplate().getSpec().getContainers().getFirst().getEnv().stream()
                                         .filter(e -> "KAFKA_BOOTSTRAP_SERVERS".equals(e.getName())).findFirst().orElseThrow().getValue());
    }
}
