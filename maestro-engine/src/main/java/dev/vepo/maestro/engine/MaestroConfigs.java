package dev.vepo.maestro.engine;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;

import dev.vepo.maestro.engine.serializers.JsonDeserializer;
import dev.vepo.maestro.engine.serializers.JsonSerde;
import dev.vepo.maestro.engine.serializers.JsonSerializer;
import org.apache.kafka.common.serialization.Serdes;

public class MaestroConfigs extends AbstractConfig {

    private static final Class<?> DEFAULT_KEY_DESERIALIZER = StringDeserializer.class;
    private static final Class<?> DEFAULT_VALUE_DESERIALIZER = JsonDeserializer.class;
    private static final Class<?> DEFAULT_KEY_SERIALIZER = StringSerializer.class;
    private static final Class<?> DEFAULT_VALUE_SERIALIZER = JsonSerializer.class;

    private static final ConfigDef CONFIG = new ConfigDef()
                                                           .define(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                                                                   Type.LIST,
                                                                   ConfigDef.NO_DEFAULT_VALUE,
                                                                   ConfigDef.ValidList.anyNonDuplicateValues(false, false),
                                                                   Importance.HIGH,
                                                                   CommonClientConfigs.BOOTSTRAP_SERVERS_DOC)
                                                           .define(StreamsConfig.APPLICATION_ID_CONFIG,
                                                                   Type.STRING,
                                                                   ConfigDef.NO_DEFAULT_VALUE,
                                                                   Importance.HIGH,
                                                                   "An identifier for the stream processing application")
                                                           .define(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                                                   Type.CLASS,
                                                                   DEFAULT_KEY_DESERIALIZER,
                                                                   Importance.HIGH,
                                                                   ConsumerConfig.KEY_DESERIALIZER_CLASS_DOC)
                                                           .define(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                                                   Type.CLASS,
                                                                   DEFAULT_VALUE_DESERIALIZER,
                                                                   Importance.HIGH,
                                                                   ConsumerConfig.VALUE_DESERIALIZER_CLASS_DOC)
                                                           .define(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                                                   Type.CLASS,
                                                                   DEFAULT_KEY_SERIALIZER,
                                                                   Importance.HIGH,
                                                                   ProducerConfig.KEY_SERIALIZER_CLASS_DOC)
                                                           .define(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                                                   Type.CLASS,
                                                                   DEFAULT_VALUE_SERIALIZER,
                                                                   Importance.HIGH,
                                                                   ProducerConfig.VALUE_SERIALIZER_CLASS_DOC);

    public static ConfigDef configDef() {
        return new ConfigDef(CONFIG);
    }

    public static Set<String> configNames() {
        return CONFIG.names();
    }

    protected MaestroConfigs(Map<?, ?> props, boolean doLog) {
        super(CONFIG, props, doLog);
    }

    public MaestroConfigs(Map<String, Object> props) {
        super(CONFIG, props);
    }

    public MaestroConfigs(Properties props) {
        super(CONFIG, props);
    }

    public StreamsConfig streams() {
        var originals = originals();
        originals.putIfAbsent(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        originals.putIfAbsent(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);
        return new StreamsConfig(originals);
    }
}
