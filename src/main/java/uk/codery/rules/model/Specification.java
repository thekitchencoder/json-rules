package uk.codery.rules.model;

import java.util.List;

public record Specification(String id, List<Rule> rules, List<RuleSet> ruleSets) {
}
