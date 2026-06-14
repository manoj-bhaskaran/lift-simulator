package com.liftsimulator.admin.service;

/**
 * Exception thrown when stored artefacts for a simulation run cannot be deleted.
 * Signals a server-side failure (e.g. file system error) during run deletion so the
 * API can surface a clear error without leaving the run in an inconsistent state.
 */
public class ArtefactDeletionException extends RuntimeException {

    public ArtefactDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
