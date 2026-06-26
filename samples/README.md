# Sample applications

Runnable Stream Language pipelines and deployment manifests. Each sample includes a `pipeline.stream` file you can pass to `maestro-app` with `--pipeline`.

## Quick run

```bash
mvn -pl maestro-app package -DskipTests

java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id my-app \
  --pipeline samples/<sample-name>/pipeline.stream
```

Create source and sink topics before starting (see each sample README).

## Samples

| Sample | Directory | Engine | Description |
|--------|-----------|--------|-------------|
| Passthrough | [passthrough/](passthrough/) | yes | Copy records from input to output |
| Filter active users | [filter-active-users/](filter-active-users/) | yes | Keep records where `status = 'active'` |
| Project user fields | [project-user-fields/](project-user-fields/) | yes | Select `user_id`, `name`, `email` |
| Orders enrichment | [orders-enrichment/](orders-enrichment/) | yes | Lookup + stream joins with project |
| Click analytics | [click-analytics/](click-analytics/) | yes | Tumbling window aggregation |
| Sensor ETL | [sensor-etl/](sensor-etl/) | yes | Map, hopping window, post-aggregate filter |
| Ad performance | [ad-performance/](ad-performance/) | yes | Stream-stream join with WITHIN |
| Stock patterns | [stock-patterns/](stock-patterns/) | yes | CEP pattern detection |
| Website sessions | [website-sessions/](website-sessions/) | yes | Sessionization + aggregates |
| Branch events | [branch-events/](branch-events/) | yes | Multi-branch routing |
| Kubernetes | [kubernetes/](kubernetes/) | — | `StreamApplication` CR example |
| Java SDK | [sdk/](sdk/) | yes | Equivalent API builder code |

**Legend:** *yes* = covered by `MaestroApplicationTest` and catalog integration tests.

## Sample input data

Samples assume JSON record values on String keys. Example record for filter/project samples:

```json
{"user_id": 101, "name": "Alice", "email": "alice@example.com", "status": "active", "age": 28}
```

Produce with `kafka-console-producer` or your preferred client.

## Adding a sample

1. Create `samples/<name>/pipeline.stream` using grammar from `Stream.g4`
2. Add `samples/<name>/README.md` with topics, example input, and runtime status
3. Register the sample in this README table
4. If the pipeline is canonical, add an entry to [docs/samples.md](../docs/samples.md)
5. Add or extend a parser or engine test when the sample represents new behavior

See [.cursor/rules/documentation.mdc](../.cursor/rules/documentation.mdc) for maintenance rules.
