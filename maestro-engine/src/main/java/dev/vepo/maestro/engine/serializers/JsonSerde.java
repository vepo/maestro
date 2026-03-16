package dev.vepo.maestro.engine.serializers;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerde implements Serde {

    @Override
    public Serializer serializer() {
        return new JsonSerializer();
    }

    @Override
    public Deserializer deserializer() {
        return new JsonDeserializer();
    }
    
}
