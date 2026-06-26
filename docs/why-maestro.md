# Why Maestro?

Kafka Streams is powerful but low-level: topologies are Java code, deployments are bespoke, and operational models vary by team. Maestro adds a **declarative layer** and **deployment framework** on top of Kafka Streams without hiding the runtime.

## Problems it solves

### 1. Topology as data, not only as code

Most teams express stream logic in imperative Java. Maestro parses a **Stream Language** into an immutable `StreamModel` — a shared contract that the parser, SDK, engine, and operator all understand. You can store pipelines in Git, review them in PRs, and deploy the same definition through JAR, Docker, or Kubernetes.

### 2. Multiple adoption paths, one model

| If you want… | Use… |
|--------------|------|
| Declarative pipelines in config files | Stream Language + `maestro-app` |
| Type-safe composition in Java | `maestro-api` SDK |
| GitOps on Kubernetes | `StreamApplication` CR + operator |
| Full control over the JVM process | `maestro-engine` directly |

Every path produces the same `StreamModel`. You are not locked into one style.

### 3. Operational consistency

The operator reconciles desired pipeline specs with running Deployments — same pipeline string the CLI accepts, same model the engine executes. Local development with the fat JAR mirrors production semantics.

### 4. Incremental language surface

The Stream Language covers common ETL patterns (filter, project, map, window, aggregate, join) with a path to advanced features (branch, pattern, sessionize). Pipelines that parse today can run tomorrow as engine support grows; unsupported stages fail fast with `UnsupportedStageException` instead of silent misbehavior.

## Design principles

**Single source of truth.** `StreamModel` in `parser.model` is the topology shape. No parallel JSON schemas or hand-rolled parsers in higher layers.

**Dependencies point down.** Operator → Application → Code → Domain. The domain module is JDK-only.

**Grammar-first DSL.** Syntax changes start in `Stream.g4`, flow through the builder and model records, then gain tests at each layer.

**Testcontainers-backed confidence.** Engine integration tests run against real Kafka, not mocks.

## When Maestro is a good fit

- You already use or plan to use **Kafka Streams** and want a declarative topology layer.
- Teams include **non-JVM specialists** who can author pipelines in the Stream Language.
- You deploy on **Kubernetes** and want CR-driven stream applications.
- You need **one topology definition** for local dev, CI, and production.

## When to look elsewhere

- You need **Flink**, **Spark Structured Streaming**, or **ksqlDB** semantics — Maestro targets Kafka Streams only.
- You require **exactly-once end-to-end** guarantees across external systems — evaluate Kafka Streams and your sink design directly; Maestro does not add new delivery semantics.
- Every stage in your pipeline is **branch / CEP / sessionization** today — those stages parse but are not yet wired in the engine (see [runtime matrix](samples.md#layer-coverage-matrix)).

## Next steps

- [Getting started](getting-started.md) — run your first pipeline in minutes
- [Sample applications](../samples/README.md) — copy-paste runnable examples
- [Stream Language reference](stream-language.md) — stage syntax and runtime status
