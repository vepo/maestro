package dev.vepo.maestro.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;

import dev.vepo.maestro.engine.MaestroConfigResolver;
import dev.vepo.maestro.engine.RuntimeConfigOverrides;
import dev.vepo.maestro.parser.StreamTopologyParser;

class MaestroAppMainTest {
    @Test
    void shouldParsePipelineSourceTopic() {
        assertThat(MaestroAppMain.parsePipelineForTest("""
                                                       FROM input
                                                       |> TO output
                                                       """)).isEqualTo("input");
    }

    @Test
    void shouldParseRepeatableStreamsPropertyFlags() {
        var options =
                MaestroAppMain.parseArgs(new String[] { "--application-id", "cli-app", "--streams-property", "num.stream.threads=4", "--streams-property", "state.dir=/tmp/state"
                });

        assertThat(options.get("application-id")).isEqualTo("cli-app");
        assertThat(options.get("streams-property")).contains("num.stream.threads=4");
        assertThat(options.get("streams-property")).contains("state.dir=/tmp/state");
    }

    @Test
    void shouldResolveConfigsFromQuerySettingsAndCli() {
        var model = new StreamTopologyParser().parse("""
                                                     FROM input
                                                     SETTINGS application.id = 'query-app', bootstrap.servers = 'kafka:9092'
                                                     |> TO output
                                                     """);

        var overrides = RuntimeConfigOverrides.builder()
                                              .bootstrapServers("kafka:9092")
                                              .property("num.stream.threads", "2")
                                              .build();
        var configs = MaestroConfigResolver.resolve(model, overrides);

        assertThat(configs.getString(StreamsConfig.APPLICATION_ID_CONFIG)).isEqualTo("query-app");
        assertThat(configs.streams().getList(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)).contains("kafka:9092");
        assertThat(configs.streams().getInt(StreamsConfig.NUM_STREAM_THREADS_CONFIG)).isEqualTo(2);
    }
}
