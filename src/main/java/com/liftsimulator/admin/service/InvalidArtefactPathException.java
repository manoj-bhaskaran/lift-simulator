package com.liftsimulator.admin.service;

/**
 * Raised when an artefact path is invalid or unsafe, typically due to path traversal attempts.
 * Error details are logged server-side but a generic message is returned to clients.
 */
public class InvalidArtefactPathException extends IllegalArgumentException {
    public InvalidArtefactPathException(String message) {
        super(message);
    }

    public InvalidArtefactPathException(String message, Throwable cause) {
        super(message, cause);
    }
}
