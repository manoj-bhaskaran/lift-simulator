package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.SimulationScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for SimulationScenarioRepository.
 * Uses H2 in-memory database configured in application-test.yml.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class SimulationScenarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SimulationScenarioRepository scenarioRepository;

    @Test
    public void testSaveAndFindById() {
        SimulationScenario scenario = new SimulationScenario(
                "Peak Hour Test",
                "{\"passengers\": 100, \"duration\": 3600}"
        );

        SimulationScenario saved = scenarioRepository.save(scenario);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("Peak Hour Test", saved.getName());
        assertEquals("{\"passengers\": 100, \"duration\": 3600}", saved.getScenarioJson());

        Optional<SimulationScenario> found = scenarioRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getName(), found.get().getName());
    }

    @Test
    public void testFindByName() {
        SimulationScenario scenario = new SimulationScenario(
                "Evening Rush",
                "{\"passengers\": 80}"
        );
        entityManager.persist(scenario);
        entityManager.flush();

        Optional<SimulationScenario> found = scenarioRepository.findByName("Evening Rush");

        assertTrue(found.isPresent());
        assertEquals("{\"passengers\": 80}", found.get().getScenarioJson());
    }

    @Test
    public void testFindByNameNotFound() {
        Optional<SimulationScenario> found = scenarioRepository.findByName("Non-existent Scenario");

        assertFalse(found.isPresent());
    }

    @Test
    public void testExistsByName() {
        SimulationScenario scenario = new SimulationScenario(
                "Morning Peak",
                "{}"
        );
        entityManager.persist(scenario);
        entityManager.flush();

        assertTrue(scenarioRepository.existsByName("Morning Peak"));
        assertFalse(scenarioRepository.existsByName("Non-existent"));
    }

    @Test
    public void testFindByNameContainingIgnoreCase() {
        SimulationScenario scenario1 = new SimulationScenario("Morning Peak Test", "{}");
        SimulationScenario scenario2 = new SimulationScenario("Evening Peak Test", "{}");
        SimulationScenario scenario3 = new SimulationScenario("Quiet Period", "{}");

        entityManager.persist(scenario1);
        entityManager.persist(scenario2);
        entityManager.persist(scenario3);
        entityManager.flush();

        List<SimulationScenario> peakScenarios = scenarioRepository
                .findByNameContainingIgnoreCase("peak");

        assertEquals(2, peakScenarios.size());
        assertTrue(peakScenarios.stream().anyMatch(s -> s.getName().equals("Morning Peak Test")));
        assertTrue(peakScenarios.stream().anyMatch(s -> s.getName().equals("Evening Peak Test")));
    }

    @Test
    public void testFindAll() {
        SimulationScenario scenario1 = new SimulationScenario("Scenario 1", "{}");
        SimulationScenario scenario2 = new SimulationScenario("Scenario 2", "{}");

        entityManager.persist(scenario1);
        entityManager.persist(scenario2);
        entityManager.flush();

        List<SimulationScenario> all = scenarioRepository.findAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    public void testUpdateScenario() {
        SimulationScenario scenario = new SimulationScenario(
                "Original Name",
                "{\"original\": true}"
        );
        entityManager.persist(scenario);
        entityManager.flush();
        entityManager.clear();

        Optional<SimulationScenario> found = scenarioRepository.findById(scenario.getId());
        assertTrue(found.isPresent());

        SimulationScenario toUpdate = found.get();
        toUpdate.setName("Updated Name");
        toUpdate.setScenarioJson("{\"updated\": true}");

        SimulationScenario updated = scenarioRepository.save(toUpdate);

        assertEquals("Updated Name", updated.getName());
        assertEquals("{\"updated\": true}", updated.getScenarioJson());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    public void testDeleteScenario() {
        SimulationScenario scenario = new SimulationScenario(
                "To Be Deleted",
                "{}"
        );
        entityManager.persist(scenario);
        entityManager.flush();

        Long id = scenario.getId();
        assertTrue(scenarioRepository.existsById(id));

        scenarioRepository.deleteById(id);

        assertFalse(scenarioRepository.existsById(id));
    }
}
