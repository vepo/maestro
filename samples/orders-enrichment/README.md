# Orders enrichment

Filters active orders and writes them to `enriched_orders`. This sample matches the operator reconciliation test for a runtime-safe pipeline.

A fuller enrichment pipeline with joins is documented in [docs/samples.md](../../docs/samples.md#stream-enrichment-through-joins).

## Topics

| Topic | Role |
|-------|------|
| `orders` | Source |
| `enriched_orders` | Sink |

## Run

```bash
kafka-topics --bootstrap-server localhost:9092 --create --topic orders --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic enriched_orders --partitions 1 --replication-factor 1

java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id orders-enrichment \
  --pipeline samples/orders-enrichment/pipeline.stream
```

## Kubernetes

```bash
kubectl apply -f samples/kubernetes/orders-filter.yaml
```

## Runtime status

**Engine:** supported (filter + sink).

**Operator:** `Synced` — see `StreamApplicationReconcilerTest.shouldReconcileBasicStreamApplication`.
