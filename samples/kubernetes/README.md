# Kubernetes samples

Deploy catalog pipelines via the `StreamApplication` custom resource. The operator watches these resources, validates the pipeline, creates a `Deployment` running `maestro-app`, and updates `.status.phase`.

## Prerequisites

```bash
# Build images (from repository root)
mvn -pl maestro-app,maestro-operator package -DskipTests
docker build -f maestro-docker/Dockerfile -t maestro-app:local .
docker build -f maestro-operator/Dockerfile -t maestro-operator:local .

# Load into Minikube (if using local cluster)
minikube image load maestro-app:local maestro-operator:local

# Install CRD and operator
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
kubectl apply -k deploy/operator/overlays/staging
```

Ensure Kafka is reachable at `kafka:9092` from the cluster (or edit `spec.kafka.bootstrapServers` in each manifest).

## Apply a catalog pipeline

| Catalog pipeline | Manifest |
|------------------|----------|
| Basic filter | [filter-active-users.yaml](filter-active-users.yaml) |
| Tumbling window aggregate | [click-analytics.yaml](click-analytics.yaml) |
| Join enrichment | [orders-enrichment.yaml](orders-enrichment.yaml) |
| Complex ETL | [sensor-etl.yaml](sensor-etl.yaml) |
| Multi-branch | [branch-events.yaml](branch-events.yaml) |
| Stream-stream join | [ad-performance.yaml](ad-performance.yaml) |
| Pattern detection | [stock-patterns.yaml](stock-patterns.yaml) |
| Sessionization | [website-sessions.yaml](website-sessions.yaml) |

Minimal getting-started example: [orders-filter.yaml](orders-filter.yaml).

```bash
kubectl apply -f samples/kubernetes/click-analytics.yaml
```

## Verify

```bash
kubectl get streamapplication click-analytics
kubectl get deployment click-analytics
kubectl get streamapplication click-analytics -o jsonpath='{.status.phase}'
```

Expected status phase: `Synced`.

Pipelines match the `.stream` files under [`samples/`](../README.md).

Adjust `spec.kafka.bootstrapServers` and `spec.image` for your cluster.
