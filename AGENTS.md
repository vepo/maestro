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
| `maestro-api` | API | exists | Public SDK for defining streams programmatically |
| `maestro-app` | Application | exists | Runnable fat JAR / main entrypoint |
| `maestro-docker` | Application | exists | Container image packaging |
| `maestro-operator` | Operator | exists | Kubernetes controller; sync model ↔ running apps |
| `maestro-crd` | Operator | exists | Custom resource definitions and schemas |

Package root: `dev.vepo.maestro`.

When adding a module, place it under the correct layer, declare dependencies only on modules from the same or lower layers, and register it in the parent `pom.xml`.

## Tech stack

- Java 25, Maven multi-module
- Coding standards: *Effective Java* 3rd ed. — all 90 items in `java-quality.mdc`
- ANTLR 4 (Stream Language), Apache Kafka Streams 4.x (runtime)
- JUnit 5, AssertJ, Awaitility, Testcontainers (tests)
- Kubernetes / Fabric8 client (operator layer)

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

1. **Read guardrails** — follow `agent-guardrails.mdc` (always applied)
2. **TDD** — write or extend a failing test first, then implement the minimum code to pass
3. **Layer-aware changes** — identify which deployment layer is affected; keep domain changes in `parser.model` and propagate upward
4. **Small diffs** — one concern per change; match existing style (records, `var`, package-private tests)
5. **Coverage** — new behavior needs tests; bug fixes need a regression test
6. **No drive-by refactors** — do not rename or restructure unrelated code
7. **Verify** — run `mvn verify` before finishing

## Agent pitfalls (quick reference)

| Pitfall | Safe approach |
|---------|---------------|
| Edit generated ANTLR files | Edit `Stream.g4` only |
| Copy DSL from disabled engine tests | Use parser tests + grammar |
| Jackson 2 imports | Jackson 3: `tools.jackson.databind` |
| Kafka types in `parser.model` | JDK-only domain |
| Skip Scenario DSL in new tests | `Scenario.given().when().then().run()` |
| Assume engine runs all stages | Passthrough only today — see `engine-runtime.mdc` |

## Test conventions

Every test is a **Gherkin-like scenario** in **domain language** — see `testing-dsl.mdc` for full rules.

- Structure: **Given → When → Then** via `Scenario.given(…).when(…).then(…).run(…)`
- Test classes: package-private, suffix `Test`, method names `should…`
- Scenario strings use domain terms (Query, source topic, sink topic, StreamModel) — not Kafka/K8s jargon
- **Code layer:** `DomainFixtures` for expected models; assert on `StreamModel` records
- **Application layer:** `StreamTestDSL` + `SendDSL` / `VerifyDSL`; Testcontainers Kafka
- **API layer:** SDK output equals equivalent Stream Language parse
- **Operator layer:** Given desired state, When reconcile, Then synced status

## Cursor rules

Project rules live in `.cursor/rules/`. **`agent-guardrails.mdc` always applies.**

| Rule | Focus |
|------|-------|
| `agent-guardrails.mdc` | AI agent pitfalls — always read first |
| `architecture.mdc` | Deployment layers, module boundaries, dependency rules |
| `antlr-grammar.mdc` | Stream.g4, canonical syntax, builder pipeline |
| `engine-runtime.mdc` | Engine MVP scope, Jackson 3, entry points |
| `maven-modules.mdc` | Module layout, pom conventions |
| `java-quality.mdc` | All 90 Effective Java items + Maestro layer conventions |
| `tdd.mdc` | Red-green-refactor workflow |
| `testing-dsl.mdc` | Gherkin-like scenario DSL and domain language in tests |
| `test-coverage.mdc` | Coverage expectations per layer |
| `ddd-domain-language.mdc` | Domain model and ubiquitous language |

When in doubt, read the relevant rule file before editing.
