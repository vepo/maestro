package dev.vepo.maestro.lang.model;

import java.util.List;

public record FunctionCallExpression(String functionName, List<Expression> arguments) implements Expression {}