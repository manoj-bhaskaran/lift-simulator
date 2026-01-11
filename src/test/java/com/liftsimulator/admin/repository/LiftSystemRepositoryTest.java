package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.LiftSystem;
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
 * Integration tests for LiftSystemRepository.
 * Uses H2 in-memory database configured in application-test.yml.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class LiftSystemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LiftSystemRepository liftSystemRepository;

    @Test
    public void testSaveAndFindById() {
        LiftSystem system = new LiftSystem(
                "test-system-1",
                "Test Lift System",
                "A test lift system"
        );

        LiftSystem saved = liftSystemRepository.save(system);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("test-system-1", saved.getSystemKey());
        assertEquals("Test Lift System", saved.getDisplayName());

        Optional<LiftSystem> found = liftSystemRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getSystemKey(), found.get().getSystemKey());
    }

    @Test
    public void testFindBySystemKey() {
        LiftSystem system = new LiftSystem(
                "unique-key",
                "Unique System",
                "System with unique key"
        );
        entityManager.persist(system);
        entityManager.flush();

        Optional<LiftSystem> found = liftSystemRepository.findBySystemKey("unique-key");

        assertTrue(found.isPresent());
        assertEquals("Unique System", found.get().getDisplayName());
    }

    @Test
    public void testFindBySystemKeyNotFound() {
        Optional<LiftSystem> found = liftSystemRepository.findBySystemKey("non-existent");

        assertFalse(found.isPresent());
    }

    @Test
    public void testExistsBySystemKey() {
        LiftSystem system = new LiftSystem(
                "existing-key",
                "Existing System",
                null
        );
        entityManager.persist(system);
        entityManager.flush();

        assertTrue(liftSystemRepository.existsBySystemKey("existing-key"));
        assertFalse(liftSystemRepository.existsBySystemKey("non-existent-key"));
    }

    @Test
    public void testFindAll() {
        LiftSystem system1 = new LiftSystem("sys-1", "System 1", "First system");
        LiftSystem system2 = new LiftSystem("sys-2", "System 2", "Second system");

        entityManager.persist(system1);
        entityManager.persist(system2);
        entityManager.flush();

        List<LiftSystem> all = liftSystemRepository.findAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    public void testUpdateLiftSystem() {
        LiftSystem system = new LiftSystem(
                "update-test",
                "Original Name",
                "Original description"
        );
        entityManager.persist(system);
        entityManager.flush();
        entityManager.clear();

        Optional<LiftSystem> found = liftSystemRepository.findBySystemKey("update-test");
        assertTrue(found.isPresent());

        LiftSystem toUpdate = found.get();
        toUpdate.setDisplayName("Updated Name");
        toUpdate.setDescription("Updated description");

        LiftSystem updated = liftSystemRepository.save(toUpdate);

        assertEquals("Updated Name", updated.getDisplayName());
        assertEquals("Updated description", updated.getDescription());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    public void testDeleteLiftSystem() {
        LiftSystem system = new LiftSystem(
                "delete-test",
                "To Be Deleted",
                "Will be deleted"
        );
        entityManager.persist(system);
        entityManager.flush();

        Long id = system.getId();
        assertTrue(liftSystemRepository.existsById(id));

        liftSystemRepository.deleteById(id);

        assertFalse(liftSystemRepository.existsById(id));
    }
}
