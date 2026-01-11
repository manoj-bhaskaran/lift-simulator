package com.liftsimulator.admin.dto;

/**
 * Represents a single validation issue (error or warning).
 */
public record ValidationIssue(
    String field,
    String message,
    Severity severity
) {
    public enum Severity {
        ERROR,
        WARNING
    }
}
