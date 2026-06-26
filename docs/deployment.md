# Deployment

How to run Maestro stream applications outside your IDE.

## Fat JAR (`maestro-app`)

The CLI entry point is `MaestroAppMain`. Package with:

```bash
mvn -pl maestro-app package -DskipTests
```

### CLI flags

| Flag | Environment variable | Description |
|------|---------------------|-------------|
| `--bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap (default `localhost:9092`) |
| `--application-id` | `APPLICATION_ID` | Kafka Streams application id |
| `--pipeline-text` | `MAESTRO_PIPELINE` | Inline Stream Language |
| `--pipeline` | — | Path to a `.stream` file |

Provide exactly one of `--pipeline-text`, `--pipeline`, or `MAESTRO_PIPELINE`.

### Example

```bash
java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers kafka:9092 \
  --application-id orders-pipeline \
  --pipeline samples/orders-enrichment/pipeline.stream
```

## Docker (`maestro-docker`)

Build from repository root after packaging the JAR:

```bash
mvn -pl maestro-app package -DskipTests
docker build -f maestro-docker/Dockerfile -t maestro-app:local .
```

Run with environment variables (no CLI flags in the container):

```bash
docker run --rm \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e APPLICATION_ID=orders-pipeline \
  -e MAESTRO_PIPELINE="$(cat samples/filter-active-users/pipeline.stream)" \
  maestro-app:local
```

See [maestro-docker/README.md](../maestro-docker/README.md).

## Kubernetes operator

### Build operator image

```bash
mvn -pl maestro-operator package -DskipTests
docker build -f maestro-operator/Dockerfile -t maestro-operator:local .
```

For Minikube, load the operator and app images:

```bash
minikube image load maestro-operator:local maestro-app:local
```

### Install CRD and operator

```bash
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
kubectl apply -k deploy/operator/overlays/staging   # or production
```

The operator runs `MaestroOperatorMain`: it watches `StreamApplication` resources cluster-wide, parses `spec.pipeline`, creates or updates a `Deployment` for `maestro-app`, and patches `.status`.

### Apply a StreamApplication

```yaml
apiVersion: maestro.dev/v1alpha1
kind: StreamApplication
metadata:
  name: orders-filter
spec:
  pipeline: |
    FROM orders
    |> FILTER WHERE status = 'active'
    |> TO enriched_orders
  kafka:
    bootstrapServers: kafka:9092
    applicationId: orders-filter
  image: maestro-app:local
```

Example manifests: [samples/kubernetes/](../samples/kubernetes/README.md) (one per catalog pipeline).

### Reconciliation behavior

| Outcome | Operator action |
|---------|-----------------|
| Pipeline parses and validates | `Synced` — `Deployment` created or updated |
| Parse or validation error | `Failed` — existing `Deployment` removed |

All catalog stages (filter, aggregate, join, branch, pattern, sessionize, …) are accepted when the DSL is valid.

The operator parses `spec.pipeline` to `StreamModel` before reconciling.

### Status

Check phase on the custom resource:

```bash
kubectl get streamapplications orders-filter -o jsonpath='{.status.phase}'
```

## Embedding `maestro-engine`

For custom lifecycle or config, depend on `maestro-engine` directly:

```xml
<dependency>
  <groupId>dev.vepo.maestro</groupId>
  <artifactId>maestro-engine</artifactId>
  <version>${maestro.version}</version>
</dependency>
```

```java
try (var app = new MaestroApplication(streamModel, new MaestroConfigs(kafkaProps))) {
    app.start();
    // block or manage lifecycle
}
```

Serde defaults: String keys, JSON values via `MaestroConfigs.streams()`.

## Configuration reference

Kafka Streams properties are passed through `MaestroConfigs` as a `Map<String, Object>`. Minimum required:

| Property | Kafka constant | Purpose |
|----------|----------------|---------|
| `bootstrap.servers` | `CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG` | Broker list |
| `application.id` | `StreamsConfig.APPLICATION_ID_CONFIG` | Consumer group / app id |

Additional Streams settings (state dir, replication factor, etc.) can be added to the same map.

## Releases

Releases are automated with GitHub Actions — see [.github/README.md](../.github/README.md) for the full pipeline.

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| **Build** | PR / push to `main` | `mvn verify` + SonarCloud |
| **Prepare Release** | Manual (`workflow_dispatch`) | Sets release version, creates `v<version>` tag, bumps patch and restores `-SNAPSHOT` on `main` |
| **Release** | Push tag `v*.*.*` | Publishes Maven artifacts to GitHub Packages, pushes the Docker image to `ghcr.io`, and attaches installers to a GitHub Release |

### Prepare a release

1. Ensure **Build** is green on `main`.
2. Go to **Actions → Prepare Release → Run workflow**.
3. On success, `main` moves to the next SNAPSHOT (e.g. `0.0.1-SNAPSHOT` → tag `v0.0.1` → `0.0.2-SNAPSHOT`).
4. The **Release** workflow runs automatically on the new tag.

### Consume published artifacts

**Maven** (add to `pom.xml`):

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/vepo/maestro</url>
</repository>
```

Authenticate with a GitHub PAT or `GITHUB_TOKEN` in `~/.m2/settings.xml` (server `id` must be `github`).

**Container:**

```bash
docker pull ghcr.io/vepo/maestro/maestro-app:<version>
```

**GitHub Release assets:** shaded `maestro-app` JAR, `maestro-operator` JAR, and `streamapplication-crd.yaml`.
