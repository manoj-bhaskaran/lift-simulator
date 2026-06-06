package com.liftsimulator.admin.service.metrics;

public final class FloorMetrics {
    private long originPassengers;
    private long destinationPassengers;
    private long liftVisits;

    public FloorMetrics(int floor) {
    }

    public void addOrigins(long count) {
        originPassengers += count;
    }

    public void addDestinations(long count) {
        destinationPassengers += count;
    }

    public void addVisit() {
        liftVisits++;
    }

    public long originPassengers() {
        return originPassengers;
    }

    public long destinationPassengers() {
        return destinationPassengers;
    }

    public long liftVisits() {
        return liftVisits;
    }
}
