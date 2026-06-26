package dev.vepo.maestro.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.streams.StreamsConfig;

import dev.vepo.maestro.engine.MaestroApplication;
import dev.vepo.maestro.engine.MaestroConfigs;
import dev.vepo.maestro.parser.StreamTopologyParser;

public final class MaestroAppMain {
    private static Map<String, Object> buildConfig(Map<String, String> options) {
        var props = new HashMap<String, Object>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getOrDefault("bootstrap-servers",
                                                                                     System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS",
                                                                                                                  "localhost:9092")));
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, options.getOrDefault("application-id",
                                                                            System.getenv().getOrDefault("APPLICATION_ID", "maestro-app")));
        return props;
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
        var configs = new MaestroConfigs(buildConfig(options));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {}));
        try (var app = new MaestroApplication(pipeline, configs)) {
            app.start();
            Thread.currentThread().join();
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        var map = new HashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                map.put(args[i].substring(2), args[++i]);
            }
        }
        return map;
    }

    static String parsePipelineForTest(String dsl) {
        return new StreamTopologyParser().parse(dsl).queries().getFirst().sourcePipeline().sourceStage().topics().getFirst();
    }
}
