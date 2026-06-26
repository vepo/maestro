package dev.vepo.maestro.parser.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record QuerySettings(Map<String, String> properties) {
    public QuerySettings {
        properties = Map.copyOf(properties);
    }

    public static QuerySettings empty() {
        return new QuerySettings(Map.of());
    }

    public static QuerySettings of(String key, String value) {
        return new QuerySettings(Map.of(key, value));
    }

    public QuerySettings merge(QuerySettings other) {
        var merged = new LinkedHashMap<>(properties);
        merged.putAll(other.properties);
        return new QuerySettings(merged);
    }
}
