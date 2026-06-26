# Maestro documentation

Maestro is a framework to **deploy Kafka Streams** using a declarative **Stream Language**. Every entry point — DSL, Java SDK, JAR, Docker, or Kubernetes operator — converges on the same `StreamModel`, so you can start simple and move up the stack without rewriting your topology.

## Start here

| Document | Audience | Contents |
|----------|----------|----------|
| [Why Maestro?](why-maestro.md) | Architects, team leads | Problems it solves, design principles, when to adopt |
| [Getting started](getting-started.md) | Developers | Build, run locally, choose a deployment layer |
| [Stream Language](stream-language.md) | Pipeline authors | Syntax, stages, runtime support matrix |
| [Deployment](deployment.md) | Platform / SRE | JAR, Docker, Kubernetes operator |
| [Sample pipelines](samples.md) | Everyone | Full pipeline catalog with layer coverage |
| [Sample applications](../samples/README.md) | Hands-on learners | Runnable `.stream` files and K8s manifests |

## Deployment layers

```
Operator (K8s CR)  →  Application (JAR/Docker)  →  Code (Stream Language)  →  StreamModel
API (Java SDK)     →  ────────────────────────────────────────────────────────→  StreamModel
```

| Layer | Module | You write… |
|-------|--------|------------|
| **Code** | `maestro-parser` | Stream Language text |
| **API** | `maestro-api` | Fluent `Maestro.stream()` builders |
| **Application** | `maestro-engine`, `maestro-app`, `maestro-docker` | Run a `StreamModel` on Kafka Streams |
| **Operator** | `maestro-crd`, `maestro-operator` | `StreamApplication` custom resources |

See [architecture](../.cursor/rules/architecture.mdc) for module boundaries and dependency rules.

## Canonical sources

When documentation and code disagree, trust these in order:

1. `maestro-parser/src/main/antlr4/dev/vepo/maestro/parser/Stream.g4` — grammar
2. Parser tests in `maestro-parser/src/test` — valid DSL examples
3. `MaestroApplicationTest` — engine runtime behavior
4. `docs/samples.md` and `samples/` — human-oriented examples (must stay in sync)

## Build

```bash
mvn verify
```
