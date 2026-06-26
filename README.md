# Maestro

[![Build](https://github.com/vepo/maestro/actions/workflows/build.yml/badge.svg)](https://github.com/vepo/maestro/actions/workflows/build.yml)

Deploy **Kafka Streams** with a declarative **Stream Language** — one `StreamModel` for DSL, Java SDK, JAR/Docker, and Kubernetes.

## Example

```text
FROM input
|> FILTER WHERE status = 'active'
|> PROJECT fields: user_id, name, email
|> TO output
```

Parsed to `StreamModel`, executed by Kafka Streams — the same definition works in the CLI, SDK, and operator. See the [Stream Language reference](docs/stream-language.md) and [sample catalog](docs/samples.md).

## Who is this for?

| You are… | Start here |
|----------|------------|
| Pipeline author | [Getting started](docs/getting-started.md) — Stream Language + fat JAR |
| Java developer | [Getting started — SDK](docs/getting-started.md#option-b--java-sdk) — `maestro-api` |
| Platform / SRE | [Deployment](docs/deployment.md) — Docker, Kubernetes operator |
| Contributor | [AGENTS.md](AGENTS.md) — architecture, tests, layer rules |
| Contributor | [CONTRIBUTING.md](CONTRIBUTING.md) — setup, workflow, PR checklist |

## Why Maestro?

- **Declarative topologies** — Stream Language instead of hand-written Kafka Streams Java
- **One model, four layers** — DSL, SDK, application runtime, and operator share `StreamModel`
- **Incremental adoption** — start with a local JAR, grow into K8s GitOps

Read [Why Maestro?](docs/why-maestro.md) for the full rationale.

## Quick start

**Prerequisites:** Java 25, Maven 3.9+, Kafka at `localhost:9092`

```bash
git clone https://github.com/vepo/maestro.git && cd maestro
mvn -pl maestro-app package -DskipTests

java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id demo \
  --pipeline samples/filter-active-users/pipeline.stream
```

Create `input` and `output` topics, then produce and consume test records — see [Getting started](docs/getting-started.md).

## How it works

```
Operator (K8s CR)  →  Application (JAR/Docker)  →  Code (Stream Language)  →  StreamModel
API (Java SDK)     →  ────────────────────────────────────────────────────────→  StreamModel
```

All paths deserialize to the same `StreamModel` — no parallel schemas.

| Layer | Module(s) | You write… |
|-------|-----------|------------|
| **Code** | `maestro-parser` | `.stream` files |
| **API** | `maestro-api` | Fluent `Maestro.stream()` builders |
| **Application** | `maestro-engine`, `maestro-app`, `maestro-docker` | Run a `StreamModel` on Kafka Streams |
| **Operator** | `maestro-crd`, `maestro-operator` | `StreamApplication` custom resources |

## Runtime support

**Runs today:** filter, project, map, window + group + aggregate, lookup/stream join, passthrough `TO`.

**Parse-only** (engine throws `UnsupportedStageException`): branch, pattern, sessionize.

Check the [layer coverage matrix](docs/samples.md#layer-coverage-matrix) before deploying a pipeline.

## Documentation

| Guide | For |
|-------|-----|
| [Documentation index](docs/index.md) | Overview of all guides |
| [Getting started](docs/getting-started.md) | First run at each deployment layer |
| [Stream Language](docs/stream-language.md) | Syntax and runtime status |
| [Deployment](docs/deployment.md) | JAR, Docker, Kubernetes |
| [Sample catalog](docs/samples.md) | Full pipeline reference |
| [Sample applications](samples/README.md) | Runnable `.stream` files and manifests |

## Docker

```bash
mvn -pl maestro-app package -DskipTests
docker build -f maestro-docker/Dockerfile -t maestro-app:local .
```

Details: [maestro-docker/README.md](maestro-docker/README.md).

## Kubernetes

```bash
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
kubectl apply -k deploy/operator/overlays/staging
kubectl apply -f samples/kubernetes/orders-filter.yaml
```

Details: [Deployment](docs/deployment.md) and [samples/kubernetes](samples/kubernetes/).

## Development

```bash
mvn verify                                    # full build + tests
mvn -pl maestro-parser test                   # parser only
mvn -pl maestro-engine -Dtest=MaestroApplicationTest test
```

Stack: Java 25, Apache Kafka Streams 4.x, ANTLR 4, JUnit 5 + Testcontainers.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup, development workflow, and pull request guidelines. [AGENTS.md](AGENTS.md) has additional architecture detail for contributors and AI agents.

## Status

Early development (`0.0.1-SNAPSHOT`) — grammar and APIs may change. Blog: [blog.vepo.dev](https://blog.vepo.dev/).
