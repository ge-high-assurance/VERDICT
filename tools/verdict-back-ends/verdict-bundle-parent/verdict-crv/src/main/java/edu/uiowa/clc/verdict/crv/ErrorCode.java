package edu.uiowa.clc.verdict.crv;

public enum ErrorCode {
    GENERIC_ATTRIBUTE_NOT_FOUND(1001, "Accessed to a generic attribute which does not exist.");

    private final int code;
    private final String description;

    private ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}
