package dev.vepo.maestro.lang.model;

import java.util.List;
import java.util.stream.Stream;

public record Sink(List<String> topic) {
    public Sink(String topic) {
        this(List.of(topic));
    }

    public Sink(String... topics) {
        this(Stream.of(topics)
                   .toList());
    }
}
