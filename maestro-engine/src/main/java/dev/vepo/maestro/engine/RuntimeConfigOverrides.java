package dev.vepo.maestro.engine;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.streams.StreamsConfig;

public final class RuntimeConfigOverrides {
    public static final class Builder {
        private final RuntimeConfigOverrides overrides = new RuntimeConfigOverrides();

        public Builder applicationId(String applicationId) {
            if (applicationId != null && !applicationId.isBlank()) {
                overrides.properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
            }
            return this;
        }

        public Builder bootstrapServers(String bootstrapServers) {
            if (bootstrapServers != null && !bootstrapServers.isBlank()) {
                overrides.properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            }
            return this;
        }

        public RuntimeConfigOverrides build() {
            return overrides;
        }

        public Builder defaultKeySerde(String className) {
            if (className != null && !className.isBlank()) {
                overrides.properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, className);
            }
            return this;
        }

        public Builder defaultValueSerde(String className) {
            if (className != null && !className.isBlank()) {
                overrides.properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, className);
            }
            return this;
        }

        public Builder property(String key, String value) {
            if (key != null && !key.isBlank() && value != null) {
                overrides.properties.put(key, value);
            }
            return this;
        }

        public Builder streamsConfigFile(String path) {
            if (path != null && !path.isBlank()) {
                overrides.streamsConfigFile = Path.of(path);
            }
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RuntimeConfigOverrides empty() {
        return new RuntimeConfigOverrides();
    }

    private final Map<String, String> properties = new LinkedHashMap<>();

    private Path streamsConfigFile;

    private RuntimeConfigOverrides() {}

    void applyTo(Map<String, Object> target) {
        if (streamsConfigFile != null) {
            MaestroConfigResolver.loadPropertiesFile(target, streamsConfigFile);
        }
        properties.forEach((key, value) -> MaestroConfigResolver.putProperty(target, key, value));
    }
}
