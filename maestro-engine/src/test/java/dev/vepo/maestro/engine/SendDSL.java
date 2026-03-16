package dev.vepo.maestro.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

class SendDSL {
    private final String topic;
    private final String bootstrapServers;
    private final List<ProducerRecord<String, String>> records;
    private String currentKey = null;

    SendDSL(String topic, String bootstrapServers) {
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
        this.records = new ArrayList<>();
    }

    SendDSL key(String key) {
        this.currentKey = key;
        return this;
    }

    SendDSL json(String json) {
        records.add(new ProducerRecord<>(topic, currentKey, json));
        this.currentKey = null; // Reset key after use
        return this;
    }

    SendDSL batch(Consumer<BatchDSL> batchConfig) {
        var batch = new BatchDSL(this);
        batchConfig.accept(batch);
        return this;
    }

    void records() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (var producer = new KafkaProducer<String, String>(props)) {
            records.forEach(producer::send);
            producer.flush();
        }
    }

    class BatchDSL {
        private final SendDSL parent;

        BatchDSL(SendDSL parent) {
            this.parent = parent;
        }

        BatchDSL json(String json) {
            parent.records.add(new ProducerRecord<>(parent.topic, parent.currentKey, json));
            return this;
        }

        BatchDSL key(String key) {
            parent.currentKey = key;
            return this;
        }
    }
}