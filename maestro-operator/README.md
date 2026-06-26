# Maestro operator

Kubernetes controller that watches `StreamApplication` custom resources and reconciles them to `Deployment` objects running `maestro-app`.

## Build

```bash
mvn -pl maestro-operator package -DskipTests
```

## Run locally

Requires a kubeconfig with the CRD installed:

```bash
kubectl apply -f maestro-crd/src/main/resources/crd/streamapplication-crd.yaml
java -jar maestro-operator/target/maestro-operator-0.0.1-SNAPSHOT.jar
```

## Docker image

From repository root:

```bash
mvn -pl maestro-operator package -DskipTests
docker build -f maestro-operator/Dockerfile -t maestro-operator:local .
```

## Deploy to cluster

```bash
kubectl apply -k deploy/operator/overlays/staging
```

See [docs/deployment.md](../docs/deployment.md) and [samples/kubernetes/](../samples/kubernetes/README.md).
