package dev.vepo.maestro.crd;

public class KafkaSpec {
    private String bootstrapServers;
    private String applicationId;

    public String getApplicationId() {
        return applicationId;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }
}
