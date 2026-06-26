package dev.vepo.maestro.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.maestro.crd.StreamApplication;
import dev.vepo.maestro.parser.StreamTopologyParser;
import dev.vepo.maestro.parser.model.BranchCase;
import dev.vepo.maestro.parser.model.BranchStage;
import dev.vepo.maestro.parser.model.ProcessingStage;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

public final class StreamApplicationReconciler {
    public record ReconcileResult(String phase, String message, Deployment deployment) {
        static ReconcileResult synced(Deployment deployment) {
            return new ReconcileResult("Synced", "Deployment reconciled", deployment);
        }

        static ReconcileResult failed(String message) {
            return new ReconcileResult("Failed", message, null);
        }
    }

    private static String namespaceOf(StreamApplication resource) {
        var namespace = resource.getMetadata().getNamespace();
        return namespace == null || namespace.isBlank() ? "default" : namespace;
    }

    private final StreamTopologyParser parser = new StreamTopologyParser();

    public Deployment buildDeployment(StreamApplication resource) {
        var name = resource.getMetadata().getName();
        var namespace = namespaceOf(resource);
        var labels = Map.of("app", name, "managed-by", "maestro-operator");
        var env = new HashMap<String, String>();
        env.put("KAFKA_BOOTSTRAP_SERVERS", resource.getSpec().getKafka().getBootstrapServers());
        env.put("APPLICATION_ID", resource.getSpec().getKafka().getApplicationId());
        env.put("MAESTRO_PIPELINE", resource.getSpec().getPipeline());

        return new DeploymentBuilder()
                                      .withNewMetadata().withName(name).withNamespace(namespace).withLabels(labels).endMetadata()
                                      .withNewSpec()
                                      .withReplicas(1)
                                      .withNewSelector().addToMatchLabels("app", name).endSelector()
                                      .withNewTemplate()
                                      .withNewMetadata().addToLabels("app", name).endMetadata()
                                      .withNewSpec()
                                      .addNewContainer()
                                      .withName("maestro")
                                      .withImage(resource.getSpec().getImage())
                                      .withEnv(env.entrySet().stream()
                                                  .map(e -> new io.fabric8.kubernetes.api.model.EnvVarBuilder()
                                                                                                               .withName(e.getKey()).withValue(e.getValue())
                                                                                                               .build())
                                                  .toList())
                                      .endContainer()
                                      .endSpec()
                                      .endTemplate()
                                      .endSpec()
                                      .build();
    }

    public ReconcileResult reconcile(StreamApplication resource) {
        try {
            var model = parser.parse(resource.getSpec().getPipeline());
            for (var query : model.queries()) {
                validateStages(query.sourcePipeline().processingStages());
            }
            var deployment = buildDeployment(resource);
            return ReconcileResult.synced(deployment);
        } catch (RuntimeException ex) {
            return ReconcileResult.failed(ex.getMessage());
        }
    }

    private void validateStages(List<ProcessingStage> stages) {
        for (ProcessingStage stage : stages) {
            if (stage instanceof BranchStage branch) {
                for (BranchCase branchCase : branch.cases()) {
                    validateStages(branchCase.stages());
                }
            }
        }
    }
}
