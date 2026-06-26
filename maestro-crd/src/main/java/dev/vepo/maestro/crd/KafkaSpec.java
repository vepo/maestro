package dev.vepo.maestro.crd;

import java.util.LinkedHashMap;
import java.util.Map;

public class KafkaSpec {
    private String bootstrapServers;
    private String applicationId;
    private String defaultKeySerde;
    private String defaultValueSerde;
    private Map<String, String> properties = new LinkedHashMap<>();

    public String getApplicationId() {
        return applicationId;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getDefaultKeySerde() {
        return defaultKeySerde;
    }

    public String getDefaultValueSerde() {
        return defaultValueSerde;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public void setDefaultKeySerde(String defaultKeySerde) {
        this.defaultKeySerde = defaultKeySerde;
    }

    public void setDefaultValueSerde(String defaultValueSerde) {
        this.defaultValueSerde = defaultValueSerde;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties == null ? new LinkedHashMap<>() : new LinkedHashMap<>(properties);
    }
}
