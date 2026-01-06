package com.liftsimulator.domain;

/**
 * Represents the lifecycle state of a lift request.
 * Requests progress through these states from creation to completion or cancellation.
 */
public enum RequestState {
    /**
     * Request has been created but not yet queued for processing.
     */
    CREATED,

    /**
     * Request is waiting in the queue to be assigned to the lift.
     */
    QUEUED,

    /**
     * Request has been assigned to the lift and is being considered for service.
     */
    ASSIGNED,

    /**
     * The lift is actively serving this request (traveling to or at the target floor).
     */
    SERVING,

    /**
     * Request has been successfully completed.
     * This is a terminal state.
     */
    COMPLETED,

    /**
     * Request has been cancelled before completion.
     * This is a terminal state.
     */
    CANCELLED
}
