package uk.codery.rules.model;

import java.util.List;

public record RuleSet(String id, Operator operator, List<Rule> rules) {
    public RuleSet(String id, List<Rule> rules){
        this(id, Operator.AND, rules);
    }
}
