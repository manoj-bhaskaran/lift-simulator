package com.liftsimulator.engine;

import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.LiftRequest;

import java.util.Set;

/**
 * Lift controller interface that exposes request management capabilities.
 */
public interface RequestManagingLiftController extends LiftController {
    void addCarCall(CarCall carCall);

    void addHallCall(HallCall hallCall);

    void addRequest(LiftRequest request);

    boolean cancelRequest(long requestId);

    void takeOutOfService();

    void returnToService();

    Set<LiftRequest> getRequests();
}
