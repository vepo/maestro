package dev.vepo.maestro.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.crd.StreamApplication;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

public final class MaestroOperatorMain {
    private static final Logger log = LoggerFactory.getLogger(MaestroOperatorMain.class);
    private static final long RESYNC_PERIOD_MS = 30_000L;

    public static void main(String[] args) throws InterruptedException {
        var reconciler = new StreamApplicationReconciler();
        try (var client = new KubernetesClientBuilder().build()) {
            var controller = new StreamApplicationController(client, reconciler);
            var informerFactory = client.informers();
            SharedIndexInformer<StreamApplication> informer =
                    informerFactory.sharedIndexInformerFor(StreamApplication.class, RESYNC_PERIOD_MS);
            informer.addEventHandler(new ResourceEventHandler<>() {
                @Override
                public void onAdd(StreamApplication resource) {
                    controller.reconcile(resource);
                }

                @Override
                public void onDelete(StreamApplication resource, boolean deletedFinalStateUnknown) {
                    controller.onDelete(resource);
                }

                @Override
                public void onUpdate(StreamApplication oldResource, StreamApplication newResource) {
                    controller.reconcile(newResource);
                }
            });
            informerFactory.startAllRegisteredInformers();
            while (!informer.hasSynced()) {
                Thread.sleep(100L);
            }
            log.info("Maestro operator started");
            Thread.currentThread().join();
        }
    }

    private MaestroOperatorMain() {}
}
