package dev.vepo.maestro.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.maestro.engine.MaestroApplication;
import dev.vepo.maestro.engine.RuntimeConfigOverrides;
import dev.vepo.maestro.parser.StreamTopologyParser;

public final class MaestroAppMain {
    private static RuntimeConfigOverrides buildOverrides(Map<String, String> options) {
        var builder = RuntimeConfigOverrides.builder()
                                            .bootstrapServers(first(options, "bootstrap-servers", "KAFKA_BOOTSTRAP_SERVERS"))
                                            .applicationId(first(options, "application-id", "APPLICATION_ID"))
                                            .defaultKeySerde(first(options, "default-key-serde", "MAESTRO_DEFAULT_KEY_SERDE"))
                                            .defaultValueSerde(first(options, "default-value-serde", "MAESTRO_DEFAULT_VALUE_SERDE"))
                                            .streamsConfigFile(first(options, "streams-config", "MAESTRO_STREAMS_CONFIG_FILE"));
        for (var property : options.getOrDefault("streams-property", "").split("\u0000")) {
            if (!property.isBlank()) {
                var separator = property.indexOf('=');
                if (separator > 0) {
                    builder.property(property.substring(0, separator), property.substring(separator + 1));
                }
            }
        }
        return builder.build();
    }

    private static String first(Map<String, String> options, String cliKey, String envKey) {
        if (options.containsKey(cliKey)) {
            return options.get(cliKey);
        }
        return System.getenv(envKey);
    }

    private static String loadPipeline(Map<String, String> options) throws Exception {
        if (options.containsKey("pipeline-text")) {
            return options.get("pipeline-text");
        }
        if (options.containsKey("pipeline")) {
            return Files.readString(Path.of(options.get("pipeline")));
        }
        var env = System.getenv("MAESTRO_PIPELINE");
        if (env != null && !env.isBlank()) {
            return env;
        }
        throw new IllegalArgumentException("Provide --pipeline, --pipeline-text, or MAESTRO_PIPELINE");
    }

    public static void main(String[] args) throws Exception {
        var options = parseArgs(args);
        var pipeline = loadPipeline(options);
        var model = new StreamTopologyParser().parse(pipeline);
        var overrides = buildOverrides(options);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {}));
        try (var app = new MaestroApplication(model, overrides)) {
            app.start();
            Thread.currentThread().join();
        }
    }

    static Map<String, String> parseArgs(String[] args) {
        var map = new HashMap<String, String>();
        List<String> streamProperties = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                continue;
            }
            var key = args[i].substring(2);
            if ("streams-property".equals(key) && i + 1 < args.length) {
                streamProperties.add(args[++i]);
            } else if (i + 1 < args.length) {
                map.put(key, args[++i]);
            }
        }
        if (!streamProperties.isEmpty()) {
            map.put("streams-property", String.join("\u0000", streamProperties));
        }
        return map;
    }

    static String parsePipelineForTest(String dsl) {
        return new StreamTopologyParser().parse(dsl).queries().getFirst().sourcePipeline().sourceStage().topics().getFirst();
    }
}
