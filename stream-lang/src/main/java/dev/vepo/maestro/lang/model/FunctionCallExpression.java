package dev.vepo.maestro.lang.model;

import java.util.List;
import java.util.stream.Stream;

public record FunctionCallExpression(String functionName, List<Expression> arguments) implements Expression {
    public FunctionCallExpression(String functionName, Expression... arguments) {
        this(functionName, Stream.of(arguments)
                                 .toList());
    }
}