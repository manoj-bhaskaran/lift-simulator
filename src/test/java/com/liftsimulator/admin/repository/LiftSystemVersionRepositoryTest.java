package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for LiftSystemVersionRepository.
 * Tests JSONB field mapping and version operations.
 * Uses H2 in-memory database with PostgreSQL compatibility mode for testing.
 */
@DataJpaTest
@ActiveProfiles("test")
public class LiftSystemVersionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LiftSystemVersionRepository versionRepository;

    private LiftSystem testSystem;

    @BeforeEach
    public void setUp() {
        testSystem = new LiftSystem(
                "test-system",
                "Test System",
                "For testing versions"
        );
        entityManager.persist(testSystem);
        entityManager.flush();
    }

    @Test
    public void testSaveVersionWithJsonbConfig() {
        String jsonConfig = "{\"floors\": 10, \"lifts\": 3, \"strategy\": \"DIRECTIONAL_SCAN\"}";

        LiftSystemVersion version = new LiftSystemVersion(testSystem, 1, jsonConfig);

        LiftSystemVersion saved = versionRepository.save(version);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(1, saved.getVersionNumber());
        assertEquals(VersionStatus.DRAFT, saved.getStatus());
        assertFalse(saved.getIsPublished());
        assertNull(saved.getPublishedAt());
        assertEquals(jsonConfig, saved.getConfig());
    }

    @Test
    public void testFindByLiftSystemIdAndVersionNumber() {
        String config = "{\"test\": \"data\"}";
        LiftSystemVersion version = new LiftSystemVersion(testSystem, 1, config);
        entityManager.persist(version);
        entityManager.flush();

        Optional<LiftSystemVersion> found = versionRepository
                .findByLiftSystemIdAndVersionNumber(testSystem.getId(), 1);

        assertTrue(found.isPresent());
        assertEquals(config, found.get().getConfig());
    }

    @Test
    public void testFindByLiftSystemIdOrderByVersionNumberDesc() {
        LiftSystemVersion v1 = new LiftSystemVersion(testSystem, 1, "{\"v\": 1}");
        LiftSystemVersion v2 = new LiftSystemVersion(testSystem, 2, "{\"v\": 2}");
        LiftSystemVersion v3 = new LiftSystemVersion(testSystem, 3, "{\"v\": 3}");

        entityManager.persist(v1);
        entityManager.persist(v2);
        entityManager.persist(v3);
        entityManager.flush();

        List<LiftSystemVersion> versions = versionRepository
                .findByLiftSystemIdOrderByVersionNumberDesc(testSystem.getId());

        assertEquals(3, versions.size());
        assertEquals(3, versions.get(0).getVersionNumber());
        assertEquals(2, versions.get(1).getVersionNumber());
        assertEquals(1, versions.get(2).getVersionNumber());
    }

    @Test
    public void testPublishVersion() {
        LiftSystemVersion version = new LiftSystemVersion(
                testSystem,
                1,
                "{\"published\": true}"
        );
        entityManager.persist(version);
        entityManager.flush();
        entityManager.clear();

        Optional<LiftSystemVersion> found = versionRepository.findById(version.getId());
        assertTrue(found.isPresent());

        LiftSystemVersion toPublish = found.get();
        toPublish.publish();

        LiftSystemVersion published = versionRepository.save(toPublish);

        assertEquals(VersionStatus.PUBLISHED, published.getStatus());
        assertTrue(published.getIsPublished());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    public void testFindByLiftSystemIdAndIsPublishedTrue() {
        LiftSystemVersion draft = new LiftSystemVersion(testSystem, 1, "{\"draft\": true}");
        LiftSystemVersion published = new LiftSystemVersion(testSystem, 2, "{\"published\": true}");
        published.publish();

        entityManager.persist(draft);
        entityManager.persist(published);
        entityManager.flush();

        List<LiftSystemVersion> publishedVersions = versionRepository
                .findByLiftSystemIdAndIsPublishedTrue(testSystem.getId());

        assertEquals(1, publishedVersions.size());
        assertEquals(2, publishedVersions.get(0).getVersionNumber());
    }

    @Test
    public void testFindByStatus() {
        LiftSystemVersion draft1 = new LiftSystemVersion(testSystem, 1, "{\"d1\": true}");
        LiftSystemVersion draft2 = new LiftSystemVersion(testSystem, 2, "{\"d2\": true}");
        LiftSystemVersion published = new LiftSystemVersion(testSystem, 3, "{\"p\": true}");
        published.publish();

        entityManager.persist(draft1);
        entityManager.persist(draft2);
        entityManager.persist(published);
        entityManager.flush();

        List<LiftSystemVersion> drafts = versionRepository.findByStatus(VersionStatus.DRAFT);
        List<LiftSystemVersion> publishedList = versionRepository.findByStatus(VersionStatus.PUBLISHED);

        assertTrue(drafts.size() >= 2);
        assertTrue(publishedList.size() >= 1);
    }

    @Test
    public void testFindMaxVersionNumberByLiftSystemId() {
        LiftSystemVersion v1 = new LiftSystemVersion(testSystem, 1, "{}");
        LiftSystemVersion v2 = new LiftSystemVersion(testSystem, 5, "{}");
        LiftSystemVersion v3 = new LiftSystemVersion(testSystem, 3, "{}");

        entityManager.persist(v1);
        entityManager.persist(v2);
        entityManager.persist(v3);
        entityManager.flush();

        Integer maxVersion = versionRepository.findMaxVersionNumberByLiftSystemId(testSystem.getId());

        assertEquals(5, maxVersion);
    }

    @Test
    public void testFindMaxVersionNumberForSystemWithNoVersions() {
        LiftSystem emptySystem = new LiftSystem(
                "empty-system",
                "Empty System",
                "No versions"
        );
        entityManager.persist(emptySystem);
        entityManager.flush();

        Integer maxVersion = versionRepository.findMaxVersionNumberByLiftSystemId(emptySystem.getId());

        assertNull(maxVersion);
    }

    @Test
    public void testArchiveVersion() {
        LiftSystemVersion version = new LiftSystemVersion(testSystem, 1, "{\"archived\": true}");
        entityManager.persist(version);
        entityManager.flush();
        entityManager.clear();

        Optional<LiftSystemVersion> found = versionRepository.findById(version.getId());
        assertTrue(found.isPresent());

        LiftSystemVersion toArchive = found.get();
        toArchive.archive();

        LiftSystemVersion archived = versionRepository.save(toArchive);

        assertEquals(VersionStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    public void testComplexJsonbConfig() {
        String complexJson = "{"
                + "\"minFloor\": 0,"
                + "\"maxFloor\": 20,"
                + "\"numLifts\": 5,"
                + "\"strategy\": \"DIRECTIONAL_SCAN\","
                + "\"liftConfigs\": ["
                + "  {\"id\": 1, \"capacity\": 10, \"speed\": 2.5},"
                + "  {\"id\": 2, \"capacity\": 8, \"speed\": 2.0}"
                + "],"
                + "\"settings\": {\"doorOpenTime\": 3, \"doorCloseTime\": 2}"
                + "}";

        LiftSystemVersion version = new LiftSystemVersion(testSystem, 1, complexJson);

        LiftSystemVersion saved = versionRepository.save(version);

        assertNotNull(saved.getId());
        assertEquals(complexJson, saved.getConfig());

        entityManager.clear();

        Optional<LiftSystemVersion> retrieved = versionRepository.findById(saved.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(complexJson, retrieved.get().getConfig());
    }

    @Test
    public void testCascadeDeleteVersionsWithParentSystem() {
        LiftSystemVersion v1 = new LiftSystemVersion(testSystem, 1, "{}");
        LiftSystemVersion v2 = new LiftSystemVersion(testSystem, 2, "{}");

        entityManager.persist(v1);
        entityManager.persist(v2);
        entityManager.flush();

        Long systemId = testSystem.getId();
        Long v1Id = v1.getId();
        Long v2Id = v2.getId();

        entityManager.clear();

        entityManager.remove(entityManager.find(LiftSystem.class, systemId));
        entityManager.flush();

        assertFalse(versionRepository.findById(v1Id).isPresent());
        assertFalse(versionRepository.findById(v2Id).isPresent());
    }
}
