# Stream Language

Maestro pipelines are **Queries**: read from source topic(s), apply processing **stages**, write to sink topic(s). Stages are chained with the pipe operator `|>`.

## Minimal pipeline

```text
FROM input_topic
|> TO output_topic
```

## Core syntax

| Construct | Form | Notes |
|-----------|------|-------|
| Source | `FROM topic_name` | One source topic per Query |
| Kafka settings | `SETTINGS key = value, ...` | Optional; after `FROM`, before first `\|>` |
| Sink | `TO topic` or `TO topic1, topic2` | Terminal stage |
| Filter | `\|> FILTER WHERE predicate` | Keep records matching predicate |
| Project | `\|> PROJECT fields: f1, f2` | Select fields from JSON values |
| Map | `\|> MAP SET field = expr, ...` | Transform or add fields |
| Window | `\|> WINDOW TUMBLING SIZE 5 MINUTES` | Tumbling, hopping, sliding |
| Group | `\|> GROUP BY field1, field2` | Required before aggregate |
| Aggregate | `\|> AGGREGATE count(*) AS n, avg(x) AS m` | After window or sessionize + group |
| Join (lookup) | `\|> JOIN users ON orders.user_id = users.id LOOKUP TABLE 'users'` | KTable lookup |
| Join (stream) | `\|> JOIN products ON ... STREAM 'topic'` | Stream-stream join |
| Join (within) | `\|> JOIN clicks ON impression_id = click_id WITHIN 30 MINUTES` | Windowed stream join |
| Branch | `\|> BRANCH CASE WHEN ... \|> ... DEFAULT \|> ...` | Multi-branch routing |
| Pattern | `\|> PATTERN ... DETECT AS alias` | CEP with WITHIN / AFTER |
| Sessionize | `\|> SESSIONIZE BY user_id GAP 30 MINUTES` | Session windows before aggregate |

Comments start with `--` and run to end of line.

## Kafka SETTINGS (per Query)

Declare Kafka Streams defaults on the Query. Keys use standard Kafka property names (`application.id`, `bootstrap.servers`, `default.value.serde`, …). Quote keys that contain reserved words (e.g. `'num.stream.threads' = 2`).

```text
FROM input
SETTINGS application.id = 'input-pipeline', 'num.stream.threads' = 2
|> FILTER WHERE status = 'active'
|> TO output
```

When a `.stream` file defines multiple Queries, the **first Query’s** `SETTINGS` apply to the shared stream application.

Override precedence (lowest → highest): **Query SETTINGS** → **environment variables** → **CLI flags** / **operator CR kafka fields**.

## Predicates

Comparisons, `AND` / `OR` / `NOT`, `IS NULL`, `IN`, `BETWEEN`, `LIKE`, and function calls are supported in `WHERE` clauses:

```text
FROM events
|> FILTER WHERE status = 'active' AND user_id IS NOT NULL
|> TO active_events
```

## Windows and time units

```text
|> WINDOW TUMBLING SIZE 5 MINUTES
|> WINDOW HOPPING SIZE 10 MINUTES ADVANCE BY 5 MINUTES
|> WINDOW SLIDING SIZE 30 SECONDS
|> SESSIONIZE BY user_id GAP 15 MINUTES TIMEOUT 1 HOUR
```

Units: `MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS` (short forms `ms`, `s`, `m`, `h`, `d` also accepted).

## Aggregations

```text
|> AGGREGATE count(*) AS total, avg(amount) AS avg_amount, max(ts) AS latest
```

Supported functions include `count`, `sum`, `avg`, `min`, `max`, `first`, `last`, and others defined in the grammar.

## Grammar source of truth

The authoritative syntax is `maestro-parser/src/main/antlr4/dev/vepo/maestro/parser/Stream.g4`. Parser tests in `SamplesParseTest` exercise the full sample catalog.

**Do not invent syntax** that is not in the grammar. If documentation and tests disagree, fix the documentation.

## Runtime support

| Stage category | Parser | Engine |
|----------------|--------|--------|
| Filter, project, map | yes | yes |
| Window, group, aggregate | yes | yes |
| Lookup / stream join | yes | yes |
| Branch, pattern, sessionize | yes | yes |

Full matrix: [samples.md — layer coverage](samples.md#layer-coverage-matrix).

## Example pipelines

Runnable files live under `samples/`. The full catalog with multi-branch and CEP examples is in [samples.md](samples.md).

**Filter + project (engine-tested):**

```text
FROM input_topic
|> FILTER WHERE status = 'active'
|> PROJECT fields: user_id, name, email
|> TO output_topic
```

**Aggregation (engine-tested):**

```text
FROM clickstream
|> FILTER WHERE event_type = 'page_view'
|> WINDOW TUMBLING SIZE 5 MINUTES
|> GROUP BY user_id, page_url
|> AGGREGATE count(*) AS view_count, avg(time_on_page) AS avg_time
|> TO analytics_topic
```

## Parsing in Java

```java
var parser = new StreamTopologyParser();
StreamModel model = parser.parse(dslString);
```

Use `StreamModel` at runtime — do not re-parse DSL inside the engine or operator.
