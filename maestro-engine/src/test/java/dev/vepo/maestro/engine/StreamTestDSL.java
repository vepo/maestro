package dev.vepo.maestro.engine;

/**
 * Domain-language aliases for topic names and publish/verify actions in tests.
 */
final class StreamTestDSL {

    static VerifyDSL expectOn(String sinkTopic, String bootstrapServers) {
        return new VerifyDSL(sinkTopic, bootstrapServers);
    }

    static SendDSL publishTo(String sourceTopic, String bootstrapServers) {
        return new SendDSL(sourceTopic, bootstrapServers);
    }

    static String sinkTopic(String name) {
        return name;
    }

    static String sourceTopic(String name) {
        return name;
    }

    private StreamTestDSL() {}
}
