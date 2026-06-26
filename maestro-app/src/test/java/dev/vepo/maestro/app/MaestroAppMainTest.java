package dev.vepo.maestro.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MaestroAppMainTest {
    @Test
    void shouldParsePipelineSourceTopic() {
        assertEquals("input", MaestroAppMain.parsePipelineForTest("""
                                                                  FROM input
                                                                  |> TO output
                                                                  """));
    }
}
