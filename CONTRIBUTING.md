# Contributing to Maestro

Thank you for your interest in Maestro. This guide covers how to set up the project, make changes safely across deployment layers, and open a pull request.

For architecture deep-dives and AI-agent conventions, see [AGENTS.md](AGENTS.md) and [`.cursor/rules/`](.cursor/rules/).

## Project overview

Maestro deploys **Kafka Streams** through a declarative **Stream Language**. Every entry point — DSL, Java SDK, fat JAR, and Kubernetes operator — converges on a single domain model: `StreamModel`.

```
Operator (K8s CR)  →  Application (JAR/Docker)  →  Code (Stream Language)  →  StreamModel
API (Java SDK)     →  ────────────────────────────────────────────────────────→  StreamModel
```

| Layer | Module(s) | Responsibility |
|-------|-----------|----------------|
| **Code** | `maestro-parser` | ANTLR grammar, parser, domain model (`parser.model`) |
| **API** | `maestro-api` | Fluent SDK that produces `StreamModel` |
| **Application** | `maestro-engine`, `maestro-app`, `maestro-docker` | Run `StreamModel` on Kafka Streams |
| **Operator** | `maestro-crd`, `maestro-operator` | Reconcile desired state with running apps |

**Golden rule:** dependencies flow **down** only. Never import a higher layer from a lower one, and never fork a parallel topology schema — all layers must use `StreamModel`.

## Prerequisites

- **Java 25** (Temurin recommended)
- **Maven 3.9+**
- **Git**
- **Docker** (for engine integration tests via Testcontainers)
- **kubectl** + a cluster (only if you work on operator samples)

## Getting started

```bash
git clone https://github.com/vepo/maestro.git
cd maestro
mvn verify
```

`mvn verify` runs the full multi-module build, unit tests, integration tests, and coverage aggregation. Run it before opening a pull request.

### Useful commands

```bash
# Full build and test
mvn verify

# Single module
mvn -pl maestro-parser test
mvn -pl maestro-engine test

# Single test class
mvn -pl maestro-parser -Dtest=BasicQueriesTest test
mvn -pl maestro-engine -Dtest=MaestroApplicationTest test

# Regenerate ANTLR output after editing Stream.g4
mvn -pl maestro-parser generate-sources test
```

## How to contribute

1. **Open an issue** (optional but helpful) to discuss larger changes before investing significant effort.
2. **Fork** the repository and create a feature branch from `main`.
3. **Make focused changes** — one concern per pull request.
4. **Add or update tests** for new behavior; add a regression test for bug fixes.
5. **Update docs and samples** when behavior, syntax, or CLI flags change (see [Documentation](#documentation)).
6. **Run `mvn verify`** locally and ensure it passes.
7. **Open a pull request** against `main` with a clear description of what changed and why.

CI runs `mvn verify` plus SonarQube analysis on every push and pull request (see [`.github/workflows/build.yml`](.github/workflows/build.yml)).

## Development workflow

We follow **test-driven development**:

1. **Red** — write a failing scenario that describes the desired behavior.
2. **Green** — implement the smallest change that makes it pass.
3. **Refactor** — clean up while keeping tests green.

Identify which **deployment layer** your change affects and propagate upward with tests at each layer touched.

### Change order by type

| Change | Typical order |
|--------|---------------|
| New Stream Language stage | `Stream.g4` → `StreamQueriesBuilder` → model record → parser test → `TopologyBuilder` → engine test → API builder → operator mapping |
| Engine stage wiring | `TopologyBuilder` → `MaestroApplicationTest` → update runtime status in docs |
| SDK builder | `maestro-api` → test that SDK output equals equivalent DSL parse |
| CLI / packaging | `maestro-app` → `docs/deployment.md`, `docs/getting-started.md` |
| Operator behavior | `maestro-operator` → `samples/kubernetes/` → `docs/deployment.md` |
| New module | layer-appropriate `pom.xml` → parent `pom.xml` → [AGENTS.md](AGENTS.md) module table |

Cross-layer features start in **domain/code** (`parser.model` and `maestro-parser`) and propagate upward. Do not bypass the parser with regex, hand-rolled JSON, or ad-hoc YAML maps.

## Testing

Every new or modified test should be a **Gherkin-like scenario** in domain language:

```java
@Test
void shouldParseQueryWithFilterStage() {
    given("a Query with a FilterStage on status")
        .when("the Stream Language is parsed")
        .then("the StreamModel contains a FilterStage")
        .run(
            () -> { },
            () -> result = parser.parse("FROM input |> FILTER WHERE status = 'active' |> TO output"),
            () -> assertThat(result).satisfies(/* … */)
        );
}
```

Conventions:

- Test classes are **package-private** (`class FooTest`, not `public class`).
- Method names: `should<Behavior>`.
- Scenario strings use domain terms (Query, source topic, sink topic, StreamModel) — not Kafka or Kubernetes jargon.
- One `@Test` per behavior.
- Use `Scenario.given().when().then().run()` from the parser or engine test sources.

| Layer | Helpers |
|-------|---------|
| Code | `Scenario`, `DomainFixtures` |
| Application | `Scenario`, `StreamTestDSL`, `SendDSL`, `VerifyDSL` |
| API | Assert SDK output equals `StreamTopologyParser` result for equivalent DSL |
| Operator | Given desired state → When reconcile → Then synced status |

**Stream Language strings in tests must match the grammar.** Canonical sources:

1. `maestro-parser/src/main/antlr4/dev/vepo/maestro/parser/Stream.g4`
2. Parser tests in `maestro-parser/src/test`

Do not copy DSL from `@Disabled` engine tests — they may use aspirational syntax not yet in the grammar.

## Code style

- Java 25, Maven multi-module layout under `dev.vepo.maestro`
- Follow *Effective Java* (3rd ed.) conventions; project-specific rules are in [`.cursor/rules/java-quality.mdc`](.cursor/rules/java-quality.mdc)
- Prefer `record`, `var`, and immutable domain types in `parser.model`
- Use SLF4J for logging in production code
- Use **Jackson 3** (`tools.jackson.databind`) — not `com.fasterxml.jackson`

## Layer boundaries

| Package / area | Allowed dependencies |
|----------------|---------------------|
| `parser.model` | JDK only — no Kafka, ANTLR, Jackson, or K8s |
| `maestro-parser` | ANTLR runtime + domain |
| `maestro-engine` | `maestro-parser`, Kafka Streams, Jackson 3 |
| `maestro-api` | `maestro-parser` |
| `maestro-operator` | application modules + K8s client |

**Never:**

- Edit generated ANTLR files under `target/` — edit `Stream.g4` and regenerate
- Add Kafka types to `parser.model`
- Import operator or engine code from `maestro-parser`
- Re-parse DSL inside `TopologyBuilder` — walk `SourcePipeline.processingStages()` from `StreamModel`

**Preferred engine entry point:** `new MaestroApplication(streamModel, configs)` — the String constructor is for CLI/bootstrap only.

## Documentation

User-facing docs live in `docs/`, `samples/`, and `README.md`. When docs disagree with code, fix the docs unless the code is wrong.

Canonical hierarchy:

1. `Stream.g4` — grammar
2. Parser tests — valid DSL examples
3. `MaestroApplicationTest` — engine runtime behavior
4. `docs/samples.md` + `samples/` — human examples and layer coverage matrix
5. `docs/stream-language.md`, `docs/getting-started.md` — guides

| Change | Also update |
|--------|-------------|
| Grammar / syntax | `docs/stream-language.md`, `docs/samples.md`, sample if user-facing |
| Engine stage | `docs/samples.md` matrix, sample README runtime status |
| SDK method | `samples/sdk/README.md`, `docs/getting-started.md` |
| CLI flag | `docs/deployment.md`, `docs/getting-started.md` |
| Operator reconcile | `docs/deployment.md`, `samples/kubernetes/README.md` |

Every `samples/*/pipeline.stream` must parse via `StreamTopologyParser`. Register new samples in `samples/README.md`.

After doc or sample changes that claim runtime support:

```bash
mvn -pl maestro-parser -Dtest=SamplesParseTest test
mvn -pl maestro-engine -Dtest=MaestroApplicationTest test   # if engine status changed
```

## Runtime status

Check the [layer coverage matrix](docs/samples.md#layer-coverage-matrix) before documenting or deploying a pipeline. Use these labels consistently:

| Label | Meaning |
|-------|---------|
| **yes** | Engine integration test passes |
| **partial** | Parses; engine wired but limited or evolving |
| **parse-only** | Parses; engine throws `UnsupportedStageException` |
| **fails reconcile** | Operator sets `Failed` phase |

## Pull request checklist

- [ ] Change is scoped to one concern
- [ ] Tests added or updated (scenario DSL for new behavior)
- [ ] `mvn verify` passes locally
- [ ] Docs and samples updated if behavior or syntax changed
- [ ] Layer boundaries respected (no upward imports, no parallel schemas)
- [ ] ANTLR changes regenerated (`mvn -pl maestro-parser generate-sources test`)

## Getting help

- [Documentation index](docs/index.md)
- [Stream Language reference](docs/stream-language.md)
- [Sample catalog](docs/samples.md)
- [AGENTS.md](AGENTS.md) — detailed architecture and conventions for contributors and AI agents

Maestro is early-stage (`0.0.1-SNAPSHOT`). Grammar and APIs may evolve — smaller, well-tested pull requests are easier to review and merge.
