# Kubernetes sample

Deploys a runtime-safe filter pipeline via the `StreamApplication` custom resource.

## Prerequisites

```bash
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
kubectl apply -k deploy/operator/overlays/staging
```

## Apply

```bash
kubectl apply -f samples/kubernetes/orders-filter.yaml
```

## Verify

```bash
kubectl get streamapplication orders-filter
kubectl get deployment orders-filter
```

Expected status phase: `Synced`.

## Pipeline

Same as [orders-enrichment](../orders-enrichment/pipeline.stream) — filter active orders to `enriched_orders`.

Adjust `spec.kafka.bootstrapServers` and `spec.image` for your cluster.
