package dev.vepo.maestro.engine.serializers;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;

public class JsonDeserializer implements Deserializer {
    private static final Logger logger = LoggerFactory.getLogger(JsonDeserializer.class);
    private final ObjectMapper mapper;

    public JsonDeserializer() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        logger.info("Deserializing {}", data);
        return mapper.readTree(data);
    }
}
