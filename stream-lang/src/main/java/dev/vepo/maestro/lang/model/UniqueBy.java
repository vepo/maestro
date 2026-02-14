package dev.vepo.maestro.lang.model;

import java.util.Collections;
import java.util.List;

public record UniqueBy(List<String> fields) {
    public static final UniqueBy EMPTY = new UniqueBy(Collections.emptyList());
}
