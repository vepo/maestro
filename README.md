# Maestro

Maestro is a framework to deploy Kafka Streams using a custom Stream Language. Users can adopt four deployment layers:

| Layer | Module | Entry point |
|-------|--------|-------------|
| API | `maestro-api` | Fluent Java SDK |
| Code | `maestro-parser` | Stream Language DSL |
| Application | `maestro-engine`, `maestro-app`, `maestro-docker` | JAR / Docker |
| Operator | `maestro-crd`, `maestro-operator` | Kubernetes `StreamApplication` CR |

See [docs/samples.md](docs/samples.md) for the canonical Stream Language.

## Build

```bash
mvn verify
```

## Run locally

```bash
mvn -pl maestro-app package -DskipTests
java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id demo \
  --pipeline-text "FROM input |> TO output"
```

## Docker

See [maestro-docker/README.md](maestro-docker/README.md).

## Kubernetes

```bash
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
kubectl apply -k deploy/operator/overlays/staging
```
