# Maestro Docker

Build the application JAR first:

```bash
mvn -pl maestro-app package -DskipTests
docker build -f maestro-docker/Dockerfile -t maestro-app:local .
```

Run:

```bash
docker run --rm \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e APPLICATION_ID=my-stream \
  -e MAESTRO_PIPELINE="FROM input |> TO output" \
  maestro-app:local
```
