package com.liftsimulator.engine;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ControllerFactoryTest {

    @Test
    public void testCreateControllerWithNearestRequestRouting() {
        LiftController controller = ControllerFactory.createController(ControllerStrategy.NEAREST_REQUEST_ROUTING);

        assertNotNull(controller);
        assertTrue(controller instanceof NaiveLiftController);
    }

    @Test
    public void testCreateControllerWithNearestRequestRoutingAndParameters() {
        LiftController controller = ControllerFactory.createController(
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                5,
                10,
                IdleParkingMode.STAY_AT_CURRENT_FLOOR
        );

        assertNotNull(controller);
        assertTrue(controller instanceof NaiveLiftController);
    }

    @Test
    public void testCreateControllerWithDirectionalScan() {
        LiftController controller = ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        assertNotNull(controller);
        assertTrue(controller instanceof DirectionalScanLiftController);
    }

    @Test
    public void testCreateControllerWithDirectionalScanAndParameters() {
        LiftController controller = ControllerFactory.createController(
                ControllerStrategy.DIRECTIONAL_SCAN,
                5,
                10,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        assertNotNull(controller);
        assertTrue(controller instanceof DirectionalScanLiftController);
    }

    @Test
    public void testCreateControllerWithNullStrategyThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ControllerFactory.createController(null)
        );

        assertEquals("Controller strategy cannot be null", exception.getMessage());
    }

    @Test
    public void testCreateControllerWithParametersAndNullStrategyThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ControllerFactory.createController(
                        null,
                        5,
                        10,
                        IdleParkingMode.PARK_TO_HOME_FLOOR
                )
        );

        assertEquals("Controller strategy cannot be null", exception.getMessage());
    }
}
