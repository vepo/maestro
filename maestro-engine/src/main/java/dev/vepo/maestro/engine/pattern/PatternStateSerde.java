package dev.vepo.maestro.engine.pattern;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import tools.jackson.databind.ObjectMapper;

final class PatternStateSerde implements Serde<PatternMatcherProcessor.PatternState> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Deserializer<PatternMatcherProcessor.PatternState> deserializer() {
        return (topic, data) -> {
            if (data == null) {
                return null;
            }
            var node = MAPPER.readTree(data);
            var state = new PatternMatcherProcessor.PatternState();
            if (node.has("matches") && node.get("matches").isObject()) {
                node.get("matches").properties().forEach(e -> state.recordMatch(e.getKey(), e.getValue().asLong()));
            }
            if (node.has("lastTimestamp")) {
                state.recordMatch("_ts", node.get("lastTimestamp").asLong());
            }
            return state;
        };
    }

    @Override
    public Serializer<PatternMatcherProcessor.PatternState> serializer() {
        return (topic, data) -> {
            if (data == null) {
                return null;
            }
            var map = new HashMap<String, Object>();
            map.put("matches", data.matches());
            map.put("lastTimestamp", data.lastTimestamp());
            return MAPPER.writeValueAsBytes(map);
        };
    }
}
