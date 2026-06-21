package com.liftsimulator.admin.service;

/**
 * Raised when the artefact system is in an invalid state, such as base path not being set.
 * Error details are logged server-side but a generic message is returned to clients.
 */
public class ArtefactStateException extends IllegalStateException {
    public ArtefactStateException(String message) {
        super(message);
    }

    public ArtefactStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
