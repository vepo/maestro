package dev.vepo.maestro.crd;

import java.util.List;
import java.util.Map;

public class StreamApplication {
    public static class Condition {
        private String type;
        private String status;
        private String message;

        public Condition() {}

        public Condition(String type, String status, String message) {
            this.type = type;
            this.status = status;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public String getStatus() {
            return status;
        }

        public String getType() {
            return type;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class KafkaSpec {
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

    public static class ObjectMeta {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class StreamApplicationSpec {
        private String pipeline;
        private KafkaSpec kafka = new KafkaSpec();
        private String image = "ghcr.io/vepo/maestro-app:latest";

        public String getImage() {
            return image;
        }

        public KafkaSpec getKafka() {
            return kafka;
        }

        public String getPipeline() {
            return pipeline;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public void setKafka(KafkaSpec kafka) {
            this.kafka = kafka;
        }

        public void setPipeline(String pipeline) {
            this.pipeline = pipeline;
        }
    }

    public static class StreamApplicationStatus {
        private String phase = "Pending";
        private List<Condition> conditions = List.of();

        public List<Condition> getConditions() {
            return conditions;
        }

        public String getPhase() {
            return phase;
        }

        public void setConditions(List<Condition> conditions) {
            this.conditions = conditions;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }
    }

    private String apiVersion = "maestro.dev/v1alpha1";

    private String kind = "StreamApplication";

    private ObjectMeta metadata = new ObjectMeta();

    private StreamApplicationSpec spec = new StreamApplicationSpec();

    private StreamApplicationStatus status = new StreamApplicationStatus();

    public String getApiVersion() {
        return apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public ObjectMeta getMetadata() {
        return metadata;
    }

    public StreamApplicationSpec getSpec() {
        return spec;
    }

    public StreamApplicationStatus getStatus() {
        return status;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public void setSpec(StreamApplicationSpec spec) {
        this.spec = spec;
    }

    public void setStatus(StreamApplicationStatus status) {
        this.status = status;
    }
}
