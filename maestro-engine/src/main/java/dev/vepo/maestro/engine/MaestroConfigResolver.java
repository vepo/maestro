package dev.vepo.maestro.engine;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.streams.StreamsConfig;

import dev.vepo.maestro.parser.model.QuerySettings;
import dev.vepo.maestro.parser.model.StreamModel;

public final class MaestroConfigResolver {
    private static final String ENV_STREAMS_CONFIG = "MAESTRO_STREAMS_CONFIG";
    private static final String ENV_STREAMS_CONFIG_FILE = "MAESTRO_STREAMS_CONFIG_FILE";
    private static final String ENV_STREAMS_PREFIX = "MAESTRO_STREAMS_";
    private static final String ENV_DEFAULT_KEY_SERDE = "MAESTRO_DEFAULT_KEY_SERDE";
    private static final String ENV_DEFAULT_VALUE_SERDE = "MAESTRO_DEFAULT_VALUE_SERDE";

    static Object coerce(String property, String value) {
        if (isSerdeProperty(property)) {
            return loadClass(value);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        if (value.matches("-?\\d+")) {
            return Integer.parseInt(value);
        }
        if (value.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(value);
        }
        return value;
    }

    static String envKeyToProperty(String envSuffix) {
        return envSuffix.toLowerCase().replace('_', '.');
    }

    private static boolean isSerdeProperty(String property) {
        return StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG.equals(property)
                || StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG.equals(property)
                || ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG.equals(property)
                || ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG.equals(property)
                || ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG.equals(property)
                || ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG.equals(property)
                || property.endsWith(".serde");
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Class not found for Kafka config: " + className, ex);
        }
    }

    static void loadPropertiesFile(Map<String, Object> target, Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            var properties = new Properties();
            properties.load(reader);
            properties.forEach((key, value) -> putProperty(target, String.valueOf(key), String.valueOf(value)));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read streams config file: " + path, ex);
        }
    }

    static void loadPropertiesString(Map<String, Object> target, String content) {
        var properties = new Properties();
        try (var reader = new StringReader(content)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid " + ENV_STREAMS_CONFIG + " content", ex);
        }
        properties.forEach((key, value) -> putProperty(target, String.valueOf(key), String.valueOf(value)));
    }

    static void mergeEnvironment(Map<String, Object> target) {
        mergeEnvironment(target, System.getenv());
    }

    static void mergeEnvironment(Map<String, Object> target, Map<String, String> environment) {
        putIfPresent(target, environment.get("KAFKA_BOOTSTRAP_SERVERS"), CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
        putIfPresent(target, environment.get("APPLICATION_ID"), StreamsConfig.APPLICATION_ID_CONFIG);
        putIfPresent(target, environment.get(ENV_DEFAULT_KEY_SERDE), StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG);
        putIfPresent(target, environment.get(ENV_DEFAULT_VALUE_SERDE), StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG);

        var inline = environment.get(ENV_STREAMS_CONFIG);
        if (inline != null && !inline.isBlank()) {
            loadPropertiesString(target, inline);
        }

        var file = environment.get(ENV_STREAMS_CONFIG_FILE);
        if (file != null && !file.isBlank()) {
            loadPropertiesFile(target, Path.of(file));
        }

        for (var entry : environment.entrySet()) {
            var key = entry.getKey();
            if (!key.startsWith(ENV_STREAMS_PREFIX) || ENV_STREAMS_CONFIG.equals(key) || ENV_STREAMS_CONFIG_FILE.equals(key)) {
                continue;
            }
            var property = envKeyToProperty(key.substring(ENV_STREAMS_PREFIX.length()));
            putProperty(target, property, entry.getValue());
        }
    }

    static void mergeQuerySettings(Map<String, Object> target, StreamModel model) {
        if (model.queries().isEmpty()) {
            return;
        }
        var settings = model.queries().getFirst().settings();
        for (var entry : settings.properties().entrySet()) {
            putProperty(target, entry.getKey(), entry.getValue());
        }
    }

    static void putIfPresent(Map<String, Object> target, String value, String property) {
        if (value != null && !value.isBlank()) {
            putProperty(target, property, value);
        }
    }

    static void putProperty(Map<String, Object> target, String property, String value) {
        target.put(property, coerce(property, value));
    }

    public static MaestroConfigs resolve(StreamModel model) {
        return resolve(model, RuntimeConfigOverrides.empty());
    }

    public static MaestroConfigs resolve(StreamModel model, RuntimeConfigOverrides overrides) {
        return resolve(model, overrides, System.getenv());
    }

    static MaestroConfigs resolve(StreamModel model, RuntimeConfigOverrides overrides, Map<String, String> environment) {
        var merged = new LinkedHashMap<String, Object>();
        mergeQuerySettings(merged, model);
        mergeEnvironment(merged, environment);
        overrides.applyTo(merged);
        return new MaestroConfigs(merged);
    }

    private MaestroConfigResolver() {}
}
