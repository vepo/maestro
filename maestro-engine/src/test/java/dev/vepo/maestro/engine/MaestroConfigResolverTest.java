package dev.vepo.maestro.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.QuerySettings;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;
import dev.vepo.maestro.engine.serializers.JsonSerde;

class MaestroConfigResolverTest {
    private static StreamModel streamModel(QuerySettings settings) {
        var merged = QuerySettings.of("bootstrap.servers", "localhost:9092")
                                  .merge(QuerySettings.of("application.id", "test-app"))
                                  .merge(settings);
        return new StreamModel(new Query(
                                         new SourcePipeline(new SourceStage("input"), List.of()),
                                         List.of("output"),
                                         merged));
    }

    @Test
    void shouldLoadInlineStreamsConfigFromEnvironment() {
        var model = streamModel(QuerySettings.empty());

        var configs = MaestroConfigResolver.resolve(model, RuntimeConfigOverrides.empty(), Map.of(
                                                                                                  "KAFKA_BOOTSTRAP_SERVERS", "localhost:9092",
                                                                                                  "MAESTRO_STREAMS_CONFIG",
                                                                                                  "state.dir=/tmp/maestro-state\n"));

        assertThat(configs.streams().getString(StreamsConfig.STATE_DIR_CONFIG)).isEqualTo("/tmp/maestro-state");
    }

    @Test
    void shouldMergeQuerySettingsEnvAndCliWithCliHighestPriority() {
        var model = streamModel(QuerySettings.of("application.id", "from-query"));
        var environment = Map.of(
                                 "APPLICATION_ID", "from-env",
                                 "MAESTRO_STREAMS_NUM_STREAM_THREADS", "3",
                                 "KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");

        var configs = MaestroConfigResolver.resolve(model, RuntimeConfigOverrides.builder()
                                                                                 .applicationId("from-cli")
                                                                                 .build(),
                                                    environment);

        assertThat(configs.getString(StreamsConfig.APPLICATION_ID_CONFIG)).isEqualTo("from-cli");
        assertThat(configs.streams().getInt(StreamsConfig.NUM_STREAM_THREADS_CONFIG)).isEqualTo(3);
    }

    @Test
    void shouldResolveSerdeClassNamesFromQuerySettings() {
        var model = streamModel(QuerySettings.of("default.value.serde", JsonSerde.class.getName()));

        var configs = MaestroConfigResolver.resolve(model, RuntimeConfigOverrides.empty(), Map.of());

        assertThat(configs.streams().getClass(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG)).isEqualTo(JsonSerde.class);
    }

    @Test
    void shouldUseQuerySettingsWhenNoOverrides() {
        var model = streamModel(QuerySettings.of("application.id", "from-query"));

        var configs = MaestroConfigResolver.resolve(model, RuntimeConfigOverrides.empty(), Map.of());

        assertThat(configs.getString(StreamsConfig.APPLICATION_ID_CONFIG)).isEqualTo("from-query");
    }
}
