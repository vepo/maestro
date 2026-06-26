package dev.vepo.maestro.crd;

import java.util.ArrayList;
import java.util.List;

public class StreamApplicationStatus {
    private String phase = "Pending";
    private List<StreamApplicationCondition> conditions = new ArrayList<>();

    public List<StreamApplicationCondition> getConditions() {
        return conditions;
    }

    public String getPhase() {
        return phase;
    }

    public void setConditions(List<StreamApplicationCondition> conditions) {
        this.conditions = conditions;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }
}
