# Filter active users

Keeps only records where the `status` field equals `'active'`.

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
  --application-id filter-demo \
  --pipeline samples/filter-active-users/pipeline.stream
```

## Test records

```bash
echo '{"id":1,"status":"active"}'   | kafka-console-producer --bootstrap-server localhost:9092 --topic input
echo '{"id":2,"status":"inactive"}' | kafka-console-producer --bootstrap-server localhost:9092 --topic input
```

Only the active record should appear on `output`.

## Runtime status

**Engine:** supported — see `MaestroApplicationTest.shouldFilterRecordsByPredicate`.
