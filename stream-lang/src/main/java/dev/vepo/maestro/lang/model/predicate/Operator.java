package dev.vepo.maestro.lang.model.predicate;

public enum Operator {
    EQUAL;

    public static Operator fromString(String operator) {
        return switch (operator) {
            case "=" -> Operator.EQUAL;
            default -> throw new IllegalArgumentException("Unknown operator! operator=%s".formatted(operator));
        };
    }
}
