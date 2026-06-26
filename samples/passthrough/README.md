# Passthrough

Copies every record from `input` to `output` unchanged.

## Topics

| Topic | Role |
|-------|------|
| `input` | Source |
| `output` | Sink |

## Run

```bash
kafka-topics --bootstrap-server localhost:9092 --create --topic input --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic output --partitions 1 --replication-factor 1

java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id passthrough-demo \
  --pipeline samples/passthrough/pipeline.stream
```

## Test record

```bash
echo '{"id":1,"message":"hello"}' | kafka-console-producer --bootstrap-server localhost:9092 --topic input
```

## Runtime status

**Engine:** supported — see `MaestroApplicationTest.shouldPassThroughRecordsFromSourceToSink`.
