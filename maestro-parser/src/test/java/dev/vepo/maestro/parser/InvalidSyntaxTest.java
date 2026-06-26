package dev.vepo.maestro.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InvalidSyntaxTest {
    private final StreamTopologyParser parser = new StreamTopologyParser();

    @Test
    void shouldRejectUnclosedString() {
        assertThrows(Exception.class, () -> parser.parse("""
                                                         FROM input
                                                         |> FILTER WHERE status = 'active
                                                         |> TO output
                                                         """));
    }
}
