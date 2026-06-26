package dev.vepo.maestro.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SamplePipelinesOnDiskTest {
    static Stream<Path> pipelineFiles() throws IOException {
        var samples = Path.of("..", "samples");
        if (!Files.isDirectory(samples)) {
            return Stream.empty();
        }
        try (var paths = Files.walk(samples)) {
            return paths.filter(p -> p.toString().endsWith(".stream")).toList().stream();
        }
    }

    private final StreamTopologyParser parser = new StreamTopologyParser();

    @ParameterizedTest
    @MethodSource("pipelineFiles")
    void shouldParseSamplePipelineFile(Path pipelineFile) throws IOException {
        var dsl = Files.readString(pipelineFile);
        assertNotNull(parser.parse(dsl), () -> "Failed to parse " + pipelineFile);
    }
}
