package uk.codery.rules.result;

public interface Result {
    String id();
    boolean matched();
    String reason();
}
