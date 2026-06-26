# Maestro

Maestro is a framework to deploy **Kafka Streams** using a declarative **Stream Language**. Define a pipeline once as `StreamModel` — then run it as a JAR, Docker container, embedded Java app, or Kubernetes custom resource.

## Why Maestro?

- **Declarative topologies** — Stream Language instead of hand-written Kafka Streams Java
- **One model, four layers** — DSL, SDK, application runtime, and operator share `StreamModel`
- **Incremental adoption** — start with a local JAR, grow into K8s GitOps

Read [docs/why-maestro.md](docs/why-maestro.md) for the full rationale.

## Documentation

| Guide | Description |
|-------|-------------|
| [Documentation index](docs/index.md) | Hub for all guides |
| [Getting started](docs/getting-started.md) | Build, run, and choose a deployment layer |
| [Stream Language](docs/stream-language.md) | Syntax and runtime support |
| [Deployment](docs/deployment.md) | JAR, Docker, Kubernetes |
| [Sample catalog](docs/samples.md) | Full pipeline reference |
| [Sample applications](samples/README.md) | Runnable `.stream` files and manifests |

## Quick start

```bash
mvn verify
mvn -pl maestro-app package -DskipTests

java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id demo \
  --pipeline samples/passthrough/pipeline.stream
```

## Deployment layers

| Layer | Module | Entry point |
|-------|--------|-------------|
| API | `maestro-api` | Fluent Java SDK |
| Code | `maestro-parser` | Stream Language DSL |
| Application | `maestro-engine`, `maestro-app`, `maestro-docker` | JAR / Docker |
| Operator | `maestro-crd`, `maestro-operator` | Kubernetes `StreamApplication` CR |

## Docker

See [maestro-docker/README.md](maestro-docker/README.md).

## Kubernetes

```bash
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
kubectl apply -k deploy/operator/overlays/staging
kubectl apply -f samples/kubernetes/orders-filter.yaml
```

## Contributing

See [AGENTS.md](AGENTS.md) for architecture, test conventions, and agent guardrails.
