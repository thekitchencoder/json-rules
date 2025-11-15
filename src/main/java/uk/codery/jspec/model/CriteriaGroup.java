package uk.codery.jspec.model;

import java.util.List;

public record CriteriaGroup(String id, Junction junction, List<Criterion> criteria) {
    public CriteriaGroup(String id, List<Criterion> criteria){
        this(id, Junction.AND, criteria);
    }
}
