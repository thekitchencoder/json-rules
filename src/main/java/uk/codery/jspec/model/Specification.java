package uk.codery.jspec.model;

import java.util.List;

public record Specification(String id, List<Criterion> criteria, List<CriteriaGroup> criteriaGroups) {
}
