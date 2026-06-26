package dev.vepo.maestro.crd;

public class StreamApplicationCondition {
    private String type;
    private String status;
    private String message;

    public StreamApplicationCondition() {}

    public StreamApplicationCondition(String type, String status, String message) {
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
