package dev.vepo.maestro.operator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.crd.StreamApplication;
import dev.vepo.maestro.crd.StreamApplicationCondition;
import dev.vepo.maestro.crd.StreamApplicationStatus;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public final class StreamApplicationController {
    private static final Logger log = LoggerFactory.getLogger(StreamApplicationController.class);
    private static final String CONDITION_TYPE = "Ready";

    private static String namespaceOf(StreamApplication resource) {
        var namespace = resource.getMetadata().getNamespace();
        return namespace == null || namespace.isBlank() ? "default" : namespace;
    }

    private final KubernetesClient client;

    private final StreamApplicationReconciler reconciler;

    public StreamApplicationController(KubernetesClient client, StreamApplicationReconciler reconciler) {
        this.client = client;
        this.reconciler = reconciler;
    }

    public void onDelete(StreamApplication resource) {
        var namespace = namespaceOf(resource);
        var name = resource.getMetadata().getName();
        client.apps().deployments().inNamespace(namespace).withName(name).delete();
        log.info("Deleted Deployment {}/{}", namespace, name);
    }

    private void patchStatus(String namespace, String name, String phase, String message) {
        var status = new StreamApplicationStatus();
        status.setPhase(phase);
        status.setConditions(List.of(new StreamApplicationCondition(
                                                                    CONDITION_TYPE,
                                                                    "Synced".equals(phase) ? "True" : "False",
                                                                    message)));

        var statusPatch = new StreamApplication();
        statusPatch.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
        statusPatch.setStatus(status);
        client.resources(StreamApplication.class).inNamespace(namespace).resource(statusPatch).patchStatus();
    }

    public void reconcile(StreamApplication resource) {
        var namespace = namespaceOf(resource);
        var name = resource.getMetadata().getName();
        var result = reconciler.reconcile(resource);

        if (result.deployment() != null) {
            var deployment = result.deployment();
            deployment.getMetadata().setNamespace(namespace);
            client.apps().deployments().inNamespace(namespace).resource(deployment).createOrReplace();
            log.info("Reconciled Deployment {}/{}", namespace, name);
        } else {
            client.apps().deployments().inNamespace(namespace).withName(name).delete();
            log.warn("Reconcile failed for {}/{}: {}", namespace, name, result.message());
        }

        patchStatus(namespace, name, result.phase(), result.message());
    }
}
