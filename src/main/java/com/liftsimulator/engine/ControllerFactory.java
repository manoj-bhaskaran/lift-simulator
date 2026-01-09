package com.liftsimulator.engine;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;

/**
 * Factory for creating LiftController instances based on the selected strategy.
 */
public class ControllerFactory {

    /**
     * Creates a LiftController using the default parameters for the given strategy.
     *
     * @param strategy the controller strategy to instantiate
     * @return a new LiftController instance
     * @throws IllegalArgumentException if the strategy is null
     * @throws UnsupportedOperationException if the strategy is not yet implemented
     */
    public static LiftController createController(ControllerStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Controller strategy cannot be null");
        }

        return switch (strategy) {
            case NEAREST_REQUEST_ROUTING -> new NaiveLiftController();
            case DIRECTIONAL_SCAN -> new DirectionalScanLiftController();
        };
    }

    /**
     * Creates a LiftController with custom parameters for the given strategy.
     *
     * @param strategy the controller strategy to instantiate
     * @param homeFloor the home floor for the controller
     * @param idleTimeoutTicks the number of ticks before the lift becomes idle
     * @param idleParkingMode the parking behavior when idle
     * @return a new LiftController instance
     * @throws IllegalArgumentException if the strategy is null
     * @throws UnsupportedOperationException if the strategy is not yet implemented
     */
    public static LiftController createController(
            ControllerStrategy strategy,
            int homeFloor,
            int idleTimeoutTicks,
            IdleParkingMode idleParkingMode) {
        if (strategy == null) {
            throw new IllegalArgumentException("Controller strategy cannot be null");
        }

        return switch (strategy) {
            case NEAREST_REQUEST_ROUTING -> new NaiveLiftController(
                    homeFloor,
                    idleTimeoutTicks,
                    idleParkingMode
            );
            case DIRECTIONAL_SCAN -> new DirectionalScanLiftController(
                    homeFloor,
                    idleTimeoutTicks,
                    idleParkingMode
            );
        };
    }
}
