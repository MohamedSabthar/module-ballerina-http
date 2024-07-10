package io.ballerina.stdlib.http.transport.message;

public enum SseFieldNames {
    EVENT("event"),
    ID("id"),
    RETRY("retry"),
    COMMENT("comment"),
    DATA("data");

    private final String fieldName;

    SseFieldNames(String name) {
        this.fieldName = name;
    }

    @Override
    public String toString() {
        return fieldName;
    }
}
