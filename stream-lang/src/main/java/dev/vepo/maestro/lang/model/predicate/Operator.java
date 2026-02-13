package dev.vepo.maestro.lang.model.predicate;

public enum Operator {
    EQUAL, NOT_EQUAL, GREATER, IN, BETWEEN;

    public static Operator fromString(String operator) {
        return switch (operator) {
            case "=" -> Operator.EQUAL;
            case ">" -> Operator.GREATER;
            case "IN" -> Operator.IN;
            default -> throw new IllegalArgumentException("Unknown operator! operator=%s".formatted(operator));
        };
    }
}
