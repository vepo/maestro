# Getting started

This guide walks through building Maestro and running a pipeline at each deployment layer.

## Prerequisites

- Java 25
- Maven 3.9+
- Kafka reachable at `localhost:9092` (or set `KAFKA_BOOTSTRAP_SERVERS`)
- For Kubernetes samples: `kubectl` and a cluster

## Build

```bash
git clone <repo-url> maestro && cd maestro
mvn verify
```

## Option A — Stream Language + fat JAR (fastest)

### 1. Package the application

```bash
mvn -pl maestro-app package -DskipTests
```

### 2. Create Kafka topics

```bash
kafka-topics --bootstrap-server localhost:9092 --create --topic input --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic output --partitions 1 --replication-factor 1
```

### 3. Run a passthrough pipeline

```bash
java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id demo-passthrough \
  --pipeline-text "FROM input |> TO output"
```

Or load a file from `samples/`:

```bash
java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id demo-filter \
  --pipeline samples/filter-active-users/pipeline.stream
```

### 4. Produce and consume test records

```bash
# Terminal 1 — already running the JAR above

# Terminal 2 — produce
echo '{"id":1,"status":"active","name":"Ada"}' | \
  kafka-console-producer --bootstrap-server localhost:9092 --topic input

# Terminal 3 — consume
kafka-console-consumer --bootstrap-server localhost:9092 --topic output --from-beginning
```

See [samples/README.md](../samples/README.md) for pipelines that filter, project, aggregate, and join.

## Option B — Java SDK

Build the same topology programmatically:

```java
import dev.vepo.maestro.api.Maestro;
import dev.vepo.maestro.engine.MaestroApplication;
import dev.vepo.maestro.engine.MaestroConfigs;
import dev.vepo.maestro.parser.model.*;

var model = Maestro.stream()
    .from("input_topic")
    .filterWhere(new ComparisonExpression(
        new FieldReferenceExpression("status"),
        ComparisonOperator.EQ,
        new LiteralExpression(new StringLiteral("active"))))
    .projectFields("user_id", "name", "email")
    .to("output_topic")
    .build();

var configs = new MaestroConfigs(Map.of(
    "bootstrap.servers", "localhost:9092",
    "application.id", "sdk-demo"));

try (var app = new MaestroApplication(model, configs)) {
    app.start();
}
```

SDK output must equal the equivalent Stream Language parse — see `SamplesApiParityTest`.

**Preferred entry point:** `MaestroApplication(StreamModel, MaestroConfigs)`. The `String` constructor parses DSL once at bootstrap/CLI only.

## Option C — Docker

```bash
mvn -pl maestro-app package -DskipTests
docker build -f maestro-docker/Dockerfile -t maestro-app:local .

docker run --rm \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e APPLICATION_ID=my-stream \
  -e MAESTRO_PIPELINE="FROM input |> TO output" \
  maestro-app:local
```

Details: [maestro-docker/README.md](../maestro-docker/README.md).

## Option D — Kubernetes operator

```bash
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
kubectl apply -k deploy/operator/overlays/staging
kubectl apply -f samples/kubernetes/orders-filter.yaml
```

The operator parses the `spec.pipeline` field, validates runtime support, and reconciles a Deployment. Pipelines with unsupported stages (e.g. `BRANCH`) receive `Failed` status.

Details: [deployment.md](deployment.md).

## Choosing a layer

| Goal | Start with |
|------|------------|
| Try Maestro in 5 minutes | Option A + `samples/passthrough` |
| Embed in an existing Java service | Option B (`maestro-engine` + `maestro-api`) |
| Container platform | Option C |
| GitOps / multi-tenant K8s | Option D |

## Runtime support

Not every Stream Language stage runs in the engine yet. Before deploying a pipeline, check the [layer coverage matrix](samples.md#layer-coverage-matrix).

**Runs today:** filter, project, map, window + group + aggregate, lookup/stream join, passthrough `TO`.

**Parse-only (engine throws `UnsupportedStageException`):** branch, pattern, sessionize.

## Next steps

- [Stream Language](stream-language.md) — syntax reference
- [Sample applications](../samples/README.md) — curated pipelines with READMEs
- [Why Maestro?](why-maestro.md) — design rationale
