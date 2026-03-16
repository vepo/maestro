package dev.vepo.maestro.engine.serializers;

import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;

public class JsonSerializer implements Serializer {
    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);
    private final ObjectMapper mapper;

    public JsonSerializer() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        logger.info("Serializing {}", data);
        return this.mapper.writeValueAsBytes(data);
    }

}
