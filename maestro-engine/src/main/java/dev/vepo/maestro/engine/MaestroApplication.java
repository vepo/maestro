package dev.vepo.maestro.engine;

import java.util.Map;
import java.util.Objects;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.maestro.parser.StreamTopologyParser;
import dev.vepo.maestro.parser.model.StreamModel;

public class MaestroApplication implements AutoCloseable {
    public enum State {
        NOT_STARTED,
        CREATED,
        REBALANCING,
        RUNNING,
        PENDING_SHUTDOWN,
        NOT_RUNNING,
        PENDING_ERROR,
        ERROR;

    }

    private static final Logger logger = LoggerFactory.getLogger(MaestroApplication.class);

    private static StreamModel load(String definition) {
        StreamTopologyParser parser = new StreamTopologyParser();
        return parser.parse(definition);
    }

    public static void main(String[] args) {
        var configs = new MaestroConfigs(Map.of());
        try (var app = new MaestroApplication(args[0], configs)) {
            app.start();
        }
    }

    private final StreamModel model;
    private final MaestroConfigs configs;

    private KafkaStreams streams;

    public MaestroApplication(StreamModel model, MaestroConfigs configs) {
        this.model = model;
        this.configs = configs;
        this.streams = null;
    }

    public MaestroApplication(StreamModel model, RuntimeConfigOverrides overrides) {
        this(model, MaestroConfigResolver.resolve(model, overrides));
    }

    public MaestroApplication(String definition, MaestroConfigs configs) {
        this(load(definition), configs);
    }

    @Override
    public void close() {
        if (Objects.nonNull(streams)) {
            streams.close();
        }
    }

    public MaestroMetrics metrics() {
        return new MaestroMetrics();
    }

    public synchronized void start() {
        if (Objects.isNull(streams)) {
            var builder = new StreamsBuilder();
            TopologyBuilder.build(model, builder);
            logger.info("Topology: {}", builder);
            this.streams = new KafkaStreams(builder.build(), configs.streams());
            this.streams.start();
        }
    }

    public State state() {
        if (Objects.nonNull(this.streams)) {
            return switch (this.streams.state()) {
                case CREATED -> State.CREATED;
                case REBALANCING -> State.REBALANCING;
                case RUNNING -> State.RUNNING;
                case PENDING_SHUTDOWN -> State.PENDING_SHUTDOWN;
                case NOT_RUNNING -> State.NOT_RUNNING;
                case PENDING_ERROR -> State.PENDING_ERROR;
                case ERROR -> State.ERROR;
            };
        } else {
            return State.NOT_STARTED;
        }
    }

    public MaestroTable table(String key) {
        return new MaestroTable(key);
    }
}