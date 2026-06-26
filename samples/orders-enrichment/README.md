# Orders enrichment

Lookup and stream joins with project to enriched output.

**Topics:** `orders`, `users`, `product_updates` → `enriched_orders`

**Engine / operator:** yes (catalog pipeline)

## Run

```bash
kafka-topics --bootstrap-server localhost:9092 --create --topic orders --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic users --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic product_updates --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic enriched_orders --partitions 1 --replication-factor 1

java -jar maestro-app/target/maestro-app-0.0.1-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --application-id orders-enrichment \
  --pipeline samples/orders-enrichment/pipeline.stream
```
