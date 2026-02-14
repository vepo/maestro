package dev.vepo.maestro.lang.model;

import java.util.List;

public record InPredicate(String fieldName, List<Literal> values) implements Expression {}