package dev.vepo.maestro.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuerySettingsParseTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldDefaultToEmptySettingsWhenOmitted() {
        Scenario.given("a pipeline without SETTINGS")
                .when("the Stream Language is parsed")
                .then("Query settings are empty")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var model = parser.parse("""
                                                  FROM input
                                                  |> TO output
                                                  """);
                         assertThat(model.queries().getFirst().settings().properties()).isEmpty();
                     });
    }

    @Test
    void shouldParseSettingsOnQuery() {
        Scenario.given("a pipeline with Kafka SETTINGS after FROM")
                .when("the Stream Language is parsed")
                .then("Query settings contain application.id and num.stream.threads")
                .run(
                     () -> {},
                     () -> {},
                     () -> {
                         var model = parser.parse("""
                                                  FROM input
                                                  SETTINGS application.id = 'input-pipeline', 'num.stream.threads' = 2
                                                  |> FILTER WHERE status = 'active'
                                                  |> TO output
                                                  """);
                         var settings = model.queries().getFirst().settings().properties();
                         assertThat(settings).containsEntry("application.id", "input-pipeline");
                         assertThat(settings).containsEntry("num.stream.threads", "2");
                     });
    }
}
