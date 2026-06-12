package com.liftsimulator.admin.controller.fixtures;

/**
 * Shared JSON fixtures for controller-level scenario and configuration validation tests.
 */
public final class ControllerApiFixtures {

    private ControllerApiFixtures() {
    }

    public static String validLiftConfig() {
        return """
            {
                "minFloor": 0,
                "maxFloor": 9,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """.trim();
    }

    public static String validScenario() {
        return """
            {
                "durationTicks": 120,
                "seed": 42,
                "passengerFlows": [
                    {
                        "startTick": 5,
                        "originFloor": 0,
                        "destinationFloor": 4,
                        "passengers": 2
                    }
                ]
            }
            """.trim();
    }

    public static String updatedScenario() {
        return """
            {
                "durationTicks": 180,
                "seed": 99,
                "passengerFlows": [
                    {
                        "startTick": 10,
                        "originFloor": 3,
                        "destinationFloor": 1,
                        "passengers": 1
                    }
                ]
            }
            """.trim();
    }

    public static String scenarioWithOutOfRangeDestination() {
        return """
            {
                "durationTicks": 120,
                "passengerFlows": [
                    {
                        "startTick": 5,
                        "originFloor": 0,
                        "destinationFloor": 12,
                        "passengers": 2
                    }
                ]
            }
            """.trim();
    }

    public static String scenarioWithUnknownProperty() {
        return """
            {
                "durationTicks": 120,
                "passengerFlows": [
                    {
                        "startTick": 5,
                        "originFloor": 0,
                        "destinationFloor": 4,
                        "passengers": 2
                    }
                ],
                "unsupportedMode": true
            }
            """.trim();
    }

    public static String configWithDomainErrors() {
        return """
            {
                "minFloor": 5,
                "maxFloor": 5,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 4,
                "homeFloor": 10,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """.trim();
    }

    public static String configWithUnknownProperty() {
        return """
            {
                "minFloor": 0,
                "maxFloor": 9,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR",
                "unexpectedField": true
            }
            """.trim();
    }
}
