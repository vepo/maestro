package dev.vepo.maestro.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.maestro.lang.model.Query;
import dev.vepo.maestro.lang.model.SourcePipeline;
import dev.vepo.maestro.lang.model.SourceStage;
import dev.vepo.maestro.lang.model.StreamModel;
import dev.vepo.maestro.lang.model.UniqueBy;

class UniqueByClauseTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldParseUniqueBySingleField() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("clickstream",
                                                                                  new UniqueBy("session_id"))),
                                               "unique_clicks")),
                     parser.parse("FROM clickstream UNIQUE BY session_id TO unique_clicks"));
    }

    @Test
    void shouldParseUniqueByMultipleFields() {
        assertEquals(new StreamModel(new Query(new SourcePipeline(new SourceStage("events",
                                                                                  new UniqueBy("user_id", "event_type", "session_id")),
                                                                  List.of()),
                                               "deduped_events")),
                     parser.parse("FROM events UNIQUE BY user_id, event_type, session_id TO deduped_events"));
    }
}
