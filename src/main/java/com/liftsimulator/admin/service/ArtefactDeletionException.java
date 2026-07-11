package com.liftsimulator.admin.service;

/**
 * Exception thrown when stored artefacts for cascade-deleted resources cannot be deleted.
 */
public class ArtefactDeletionException extends RuntimeException {

    public ArtefactDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
