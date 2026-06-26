# Maestro — Agent Guide

Maestro is a **framework to deploy Kafka Streams** using a custom **Stream Language**. Users can adopt the framework at different levels of abstraction — from an SDK in code to a Kubernetes operator — without changing the underlying stream model.

## Deployment layers

Maestro is organized in four deployment layers. Each layer is a valid entry point; lower layers are building blocks for higher ones.

| Layer | User experience | Responsibility |
|-------|-----------------|----------------|
| **API** | Programmatic SDK | Compose and configure streams in Java without hand-writing DSL strings |
| **Code** | Stream Language | Declare topologies in the Maestro Stream Language (parsed to domain model) |
| **Application** | JAR or Docker | Run a stream as a standalone process (Kafka Streams runtime) |
| **Operator** | Kubernetes resources | Deploy applications and reconcile desired model with running elements |

```
                    ┌─────────────────────────────────┐
                    │  Operator Layer (Kubernetes)    │
                    │  CRD → reconcile → running app  │
                    └───────────────┬─────────────────┘
                                    │ deploys
                    ┌───────────────▼─────────────────┐
                    │  Application Layer (JAR/Docker) │
                    │  StreamModel → Kafka Streams    │
                    └───────────────┬─────────────────┘
                                    │ executes
              ┌─────────────────────┴─────────────────────┐
              │           StreamModel (domain)            │
              └─────────────▲───────────────▲─────────────┘
                            │               │
              ┌─────────────┴───┐   ┌───────┴──────────┐
              │  Code Layer     │   │  API Layer (SDK) │
              │  Stream Language│   │  fluent builders │
              └─────────────────┘   └──────────────────┘
```

**Golden rule:** `StreamModel` is the shared contract across all layers. Parsing, SDK builders, and operator specs must converge on the same domain types — never fork parallel representations.

## Repository layout

The repo is a Maven multi-module project. Modules are added per layer as needed; depend **down** the stack, never up.

| Module | Layer | Status | Role |
|--------|-------|--------|------|
| `maestro-parser` | Code | exists | ANTLR grammar, parser, domain model (`parser.model`) |
| `maestro-engine` | Application | exists | Kafka Streams runtime; executes `StreamModel` |
| `maestro-api` | API | planned | Public SDK for defining streams programmatically |
| `maestro-app` | Application | planned | Runnable fat JAR / main entrypoint |
| `maestro-docker` | Application | planned | Container image packaging |
| `maestro-operator` | Operator | planned | Kubernetes controller; sync model ↔ running apps |
| `maestro-crd` | Operator | planned | Custom resource definitions and schemas |

Package root: `dev.vepo.maestro`.

When adding a module, place it under the correct layer, declare dependencies only on modules from the same or lower layers, and register it in the parent `pom.xml`.

## Tech stack

- Java 25, Maven multi-module
- ANTLR 4 (Stream Language), Apache Kafka Streams 4.x (runtime)
- JUnit 5, AssertJ, Awaitility, Testcontainers (tests)
- Kubernetes / Java Operator SDK (planned, operator layer)

## Commands

```bash
# Full build and test
mvn verify

# Single module
mvn -pl maestro-parser test
mvn -pl maestro-engine test

# Single test class
mvn -pl maestro-parser -Dtest=BasicQueriesTest test
```

Always run `mvn verify` before finishing a change that touches production code.

## Dependency direction

```
Operator  →  Application  →  Code  →  Domain (parser.model)
API       →  Code         →  Domain
```

- **Domain** (`parser.model`): JDK only — no Kafka, ANTLR, K8s, or SDK imports
- **Code** (`maestro-parser`): language + parser; no Kafka runtime
- **Application** (`maestro-engine`, …): executes model on Kafka Streams
- **API** (`maestro-api`, …): user-facing builders; produces `StreamModel`
- **Operator** (`maestro-operator`, …): reads desired state from K8s, drives application layer

Higher layers must not be imported by lower layers.

## Domain language (Ubiquitous Language)

Use these terms consistently in code, tests, docs, and CRDs:

| Term | Meaning |
|------|---------|
| Stream Language | Maestro DSL for declaring stream topologies |
| StreamModel | Root aggregate — canonical representation shared by all layers |
| Query | One `FROM … TO …` pipeline definition |
| SourcePipeline / SourceStage | Input topic(s) and upstream stages |
| Sink topics | Output topic names (`TO` clause) |
| Stage | Processing step: Filter, Project, Join, Window, Aggregate, etc. |
| Stream Application | A running Kafka Streams process built from a `StreamModel` |
| Desired state | The `StreamModel` (or spec) a layer intends to run |
| Reconciliation | Operator action that aligns cluster state with desired state |

Prefer domain names over infrastructure jargon in model and API code. Map to Kafka/K8s terms only at the application or operator boundary.

## Development workflow

1. **TDD** — write or extend a failing test first, then implement the minimum code to pass
2. **Layer-aware changes** — identify which deployment layer is affected; keep domain changes in `parser.model` and propagate upward
3. **Small diffs** — one concern per change; match existing style (records, `var`, package-private tests)
4. **Coverage** — new behavior needs tests; bug fixes need a regression test
5. **No drive-by refactors** — do not rename or restructure unrelated code

## Test conventions

- Test classes: package-private, suffix `Test`, method names `should…`
- **Code layer:** assert full domain objects (`assertEquals` on records), not string fragments
- **Application layer:** Testcontainers Kafka; AssertJ and Awaitility for async assertions
- **API layer:** test that SDK output equals equivalent parser output for the same topology
- **Operator layer:** unit-test reconciliation logic; use envtest or mock K8s client for integration
- Test DSL helpers live in test sources (`SendDSL`, `VerifyDSL`)

## Cursor rules

Project rules live in `.cursor/rules/`:

| Rule | Focus |
|------|-------|
| `architecture.mdc` | Deployment layers, module boundaries, dependency rules |
| `java-quality.mdc` | Java style, immutability, logging, error handling |
| `tdd.mdc` | Red-green-refactor workflow |
| `test-coverage.mdc` | Coverage expectations per layer |
| `ddd-domain-language.mdc` | Domain model and ubiquitous language |

When in doubt, read the relevant rule file before editing.
