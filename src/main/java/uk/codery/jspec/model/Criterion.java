package uk.codery.jspec.model;

import java.util.Collections;
import java.util.Map;

public record Criterion(String id, Map<String, Object> query) {
    public Criterion(String id) {
        this(id, Collections.emptyMap());
    }
}
