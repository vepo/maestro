package dev.vepo.maestro.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.parser.model.Duration;
import dev.vepo.maestro.parser.model.Query;
import dev.vepo.maestro.parser.model.SourcePipeline;
import dev.vepo.maestro.parser.model.SourceStage;
import dev.vepo.maestro.parser.model.StreamModel;
import dev.vepo.maestro.parser.model.TimeUnit;
import dev.vepo.maestro.parser.model.WindowStage;
import dev.vepo.maestro.parser.model.WindowType;

class WindowStagesTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseTumblingWindow() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("events"),
                                                                  new WindowStage(WindowType.TUMBLING,
                                                                                  new Duration(5, TimeUnit.MINUTES))),
                                               "windowed")),
                     parser.parse("FROM events |> WINDOW TUMBLING 5 MINUTES TO windowed"));
    }

    @Test
    void shouldParseSlidingWindow() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("metrics"),
                                                                  List.of(new WindowStage(WindowType.SLIDING,
                                                                                          new Duration(2, TimeUnit.SECONDS),
                                                                                          new Duration(10, TimeUnit.SECONDS)))),
                                               "sliding")),
                     parser.parse("""
                                  FROM metrics
                                  |> WINDOW SLIDING 10 SECONDS EVERY 2 SECONDS

                                  TO sliding
                                  """));
    }

    @Test
    void shouldParseSessionWindow() {
        assertEquals(new StreamModel(new Query(
                                               new SourcePipeline(new SourceStage("user_activity"),
                                                                  List.of(new WindowStage(WindowType.SESSION,
                                                                                          new Duration(30, TimeUnit.MINUTES)))),
                                               "sessions")),
                     parser.parse("FROM user_activity |> WINDOW SESSION 30 MINUTES TO sessions"));
    }

    @Test
    void shouldParseAllTimeUnits() {
        var expected = new StreamModel(new Query(
                                                 new SourcePipeline(new SourceStage("test"),
                                                                    List.of(new WindowStage(WindowType.TUMBLING,
                                                                                            new Duration(1, TimeUnit.MILLISECONDS)))),
                                                 "out"));

        assertEquals(expected, parser.parse("FROM test |> WINDOW TUMBLING 1 MILLISECONDS TO out"));
        assertEquals(new StreamModel(new Query(
                                               new SourcePipeline(new SourceStage("test"),
                                                                  List.of(new WindowStage(WindowType.TUMBLING,
                                                                                          new Duration(1, TimeUnit.HOURS)))),
                                               "out")),
                     parser.parse("FROM test |> WINDOW TUMBLING 1 HOURS TO out"));
        assertEquals(new StreamModel(new Query(
                                               new SourcePipeline(new SourceStage("test"),
                                                                  List.of(new WindowStage(WindowType.TUMBLING,
                                                                                          new Duration(1, TimeUnit.DAYS)))),
                                               "out")),
                     parser.parse("FROM test |> WINDOW TUMBLING 1 DAYS TO out"));
    }
}
