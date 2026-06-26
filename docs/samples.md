# Sample pipeline catalog

Canonical Stream Language examples for documentation and tests. Runnable copies live under [`samples/`](../samples/README.md).

Grammar source of truth: `maestro-parser/src/main/antlr4/dev/vepo/maestro/parser/Stream.g4`

## Basic pipeline with multiple stages

Parses and runs in the engine. Sample: [`samples/filter-active-users`](../samples/filter-active-users/).

```text
FROM input_topic
|> FILTER WHERE status = 'active'
|> PROJECT fields: user_id, name, email
|> TO output_topic
```

## Aggregation pipeline

Parses; engine support is partial. Sample: [`samples/click-analytics`](../samples/click-analytics/).

```text
FROM clickstream
|> FILTER WHERE event_type = 'page_view'
|> WINDOW TUMBLING SIZE 5 MINUTES
|> GROUP BY user_id, page_url
|> AGGREGATE count(*) AS view_count, avg(time_on_page) AS avg_time
|> TO analytics_topic
```

## Stream enrichment through joins

```text
FROM orders
|> JOIN users ON orders.user_id = users.id LOOKUP TABLE 'users'
|> JOIN products ON orders.product_id = products.id STREAM 'product_updates'
|> PROJECT enriched_order: order_id, user_name, product_name, quantity, price
|> TO enriched_orders
```

## Complex ETL pipeline

```text
FROM raw_sensor_data
|> FILTER WHERE temperature IS NOT NULL AND status != 'error'
|> MAP
   SET normalized_temp = (temperature - 32) * 5/9,
   timestamp = to_epoch_ms(timestamp)
|> WINDOW HOPPING SIZE 10 MINUTES ADVANCE BY 5 MINUTES
|> GROUP BY device_id, sensor_type
|> AGGREGATE
   avg(normalized_temp) AS avg_temp,
   max(normalized_temp) AS max_temp,
   min(normalized_temp) AS min_temp,
   count(*) AS reading_count
|> FILTER WHERE reading_count > 10
|> TO device_analytics, alert_topic
```

## Multi-branch pipeline

Parse-only at engine runtime. Sample: [`samples/branch-events`](../samples/branch-events/).

```text
FROM user_events
|> BRANCH
   CASE WHEN event_type = 'purchase'
        |> PROJECT purchase_data
        |> TO purchase_topic,
   CASE WHEN event_type = 'click'
        |> WINDOW TUMBLING SIZE 1 HOUR
        |> GROUP BY user_id
        |> AGGREGATE count(*) AS click_count
        |> TO click_analytics,
   DEFAULT
        |> TO other_events_topic
```

## Stream-stream join with window

```text
FROM ad_impressions
|> WINDOW TUMBLING SIZE 1 HOUR
|> JOIN ad_clicks ON impression_id = click_id WITHIN 30 MINUTES
|> PROJECT
   ad_id,
   impression_time,
   click_time,
   time_to_click = click_time - impression_time
|> TO ad_performance
```

## Pattern detection

Parse-only at engine runtime.

```text
FROM stock_ticks
|> PATTERN
   DROP = price < stop_loss,
   REBOUND = price > take_profit WITHIN 5 MINUTES AFTER DROP
|> DETECT AS volatility_event
|> TO alerts
```

## Sessionization

Parse-only at engine runtime.

```text
FROM website_activity
|> SESSIONIZE BY user_id
   GAP 30 MINUTES
   TIMEOUT 2 HOURS
|> AGGREGATE
   count(*) AS actions_per_session,
   sum(page_views) AS total_views,
   first(action_time) AS session_start,
   last(action_time) AS session_end
|> TO sessions_topic
```

## Layer coverage matrix

| Sample pipeline | Parser | Engine | API | Operator |
|-----------------|--------|--------|-----|----------|
| Basic filter + project | yes | yes | yes | yes |
| Aggregation (tumbling window) | yes | partial | partial | reconcile |
| Stream enrichment (joins) | yes | partial | partial | reconcile |
| Complex ETL (map + hopping + aggregate) | yes | partial | partial | reconcile |
| Multi-branch | yes | no (parse-only) | partial | fails reconcile |
| Stream-stream join | yes | partial | partial | reconcile |
| Pattern detection | yes | no (parse-only) | partial | fails reconcile |
| Sessionization | yes | no (parse-only) | partial | fails reconcile |

**Legend:** *yes* = tested or fully wired; *partial* = parses and/or partial runtime; *no* = `UnsupportedStageException` at engine runtime; *reconcile* = operator creates Deployment when pipeline is runtime-safe; *fails reconcile* = operator sets `Failed` phase.

Update this matrix when `TopologyBuilder` or operator validation changes.

## Test references

| Pipeline | Test class |
|----------|------------|
| All catalog entries | `SamplesParseTest` |
| Filter, project, passthrough | `MaestroApplicationTest` |
| Branch, pattern, sessionize (rejection) | `MaestroApplicationTest` |
| SDK parity (basic) | `SamplesApiParityTest` |
| Operator reconcile / fail | `StreamApplicationReconcilerTest` |
