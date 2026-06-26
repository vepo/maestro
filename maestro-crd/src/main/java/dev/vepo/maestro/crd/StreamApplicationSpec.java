package dev.vepo.maestro.crd;

public class StreamApplicationSpec {
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
