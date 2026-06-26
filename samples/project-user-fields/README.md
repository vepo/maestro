# Project user fields

Selects `user_id`, `name`, and `email` from each JSON record. Other fields (e.g. `age`) are dropped from the output.

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
  --application-id project-demo \
  --pipeline samples/project-user-fields/pipeline.stream
```

## Test record

```bash
echo '{"user_id":101,"name":"Alice","email":"alice@example.com","age":28}' | \
  kafka-console-producer --bootstrap-server localhost:9092 --topic input
```

Output should contain `user_id`, `name`, and `email` but not `age`.

## Runtime status

**Engine:** supported — see `MaestroApplicationTest.shouldProjectSelectedFields`.
