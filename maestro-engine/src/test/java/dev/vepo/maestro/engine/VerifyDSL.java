package dev.vepo.maestro.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

class VerifyDSL {
    private final String topic;
    private final String bootstrapServers;
    private Duration timeout = Duration.ofSeconds(10);
    private Duration pollInterval = Duration.ofMillis(500);
    private int expectedCount = -1;

    VerifyDSL(String topic, String bootstrapServers) {
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
    }

    VerifyDSL within(long duration, TimeUnit unit) {
        this.timeout = Duration.ofNanos(unit.toNanos(duration));
        return this;
    }

    VerifyDSL within(Duration duration) {
        this.timeout = duration;
        return this;
    }

    VerifyDSL withTimeout(long duration, TimeUnit unit) {
        return within(duration, unit);
    }

    VerifyDSL withPollInterval(long duration, TimeUnit unit) {
        this.pollInterval = Duration.ofNanos(unit.toNanos(duration));
        return this;
    }

    VerifyDSL received(int count) {
        this.expectedCount = count;
        return this;
    }

    RecordAssertion records() {
        return new RecordAssertion(this);
    }

    private List<ConsumerRecord<String, String>> consumeRecords() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.currentTimeMillis());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(topic));

            ConditionFactory await = Awaitility.await()
                                               .atMost(timeout)
                                               .pollInterval(pollInterval);

            if (expectedCount > 0) {
                await.until(() -> {
                    var polled = consumer.poll(Duration.ofMillis(100));
                    polled.records(topic).forEach(records::add);
                    return records.size() >= expectedCount;
                });
            } else {
                await.until(() -> {
                    var polled = consumer.poll(Duration.ofMillis(100));
                    polled.records(topic).forEach(records::add);
                    return !records.isEmpty();
                });
            }
        }
        return records;
    }

    class RecordAssertion {
        private final VerifyDSL verify;
        private List<ConsumerRecord<String, String>> records;

        RecordAssertion(VerifyDSL verify) {
            this.verify = verify;
        }

        RecordAssertion assertThat(Consumer<List<ConsumerRecord<String, String>>> assertion) {
            this.records = verify.consumeRecords();

            if (verify.expectedCount > 0) {
                Assertions.assertThat(records).hasSize(verify.expectedCount);
            }

            assertion.accept(records);
            return this;
        }

        RecordAssertion assertFirst(Consumer<ConsumerRecord<String, String>> assertion) {
            this.records = verify.consumeRecords();
            Assertions.assertThat(records).isNotEmpty();
            assertion.accept(records.get(0));
            return this;
        }

        RecordAssertion assertAll(Consumer<ConsumerRecord<String, String>> assertion) {
            this.records = verify.consumeRecords();
            records.forEach(assertion);
            return this;
        }

        List<ConsumerRecord<String, String>> get() {
            if (records == null) {
                records = verify.consumeRecords();
            }
            return records;
        }
    }
}