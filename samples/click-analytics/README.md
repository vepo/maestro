# Click analytics

Counts page views per user and URL in 5-minute tumbling windows.

## Topics

| Topic | Role |
|-------|------|
| `clickstream` | Source |
| `analytics_topic` | Sink |

## Run

```bash
kafka-topics --bootstrap-server localhost:9092 --create --topic clickstream --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic analytics_topic --partitions 1 --replication-factor 1

java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id click-analytics \
  --pipeline samples/click-analytics/pipeline.stream
```

## Test records

```bash
echo '{"event_type":"page_view","user_id":"u1","page_url":"/home","time_on_page":30}' | \
  kafka-console-producer --bootstrap-server localhost:9092 --topic clickstream
```

## Runtime status

**Parser:** supported — `SamplesParseTest.shouldParseAggregationPipeline`.

**Engine:** partial — window, group, and aggregate stages are wired but behavior may evolve; verify with integration tests before production use.
