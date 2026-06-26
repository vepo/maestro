package dev.vepo.maestro.engine;

public class UnsupportedStageException extends RuntimeException {
    public UnsupportedStageException(String stageType) {
        super("Stage not supported at runtime: " + stageType);
    }
}
