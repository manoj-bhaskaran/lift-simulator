package com.liftsimulator.admin.runner;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Command-line runner to verify basic JPA entity and repository operations.
 * Enable with: --verify-jpa=true or spring.jpa.verify=true
 */
@Component
@ConditionalOnProperty(name = "spring.jpa.verify", havingValue = "true", matchIfMissing = false)
public class JpaVerificationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(JpaVerificationRunner.class);

    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository versionRepository;

    public JpaVerificationRunner(
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository versionRepository) {
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("=== Starting JPA Entity and Repository Verification ===");

        try {
            verifyLiftSystemOperations();
            verifyLiftSystemVersionOperations();
            verifyJsonbMapping();
            verifyRelationships();
            verifyQueryMethods();

            logger.info("=== JPA Verification Completed Successfully ===");
        } catch (Exception e) {
            logger.error("=== JPA Verification Failed ===", e);
            throw e;
        }
    }

    private void verifyLiftSystemOperations() {
        logger.info("--- Verifying LiftSystem CRUD Operations ---");

        LiftSystem system = new LiftSystem(
                "demo-system",
                "Demo Lift System",
                "A demonstration lift system for JPA verification"
        );

        LiftSystem saved = liftSystemRepository.save(system);
        logger.info("✓ Created LiftSystem: id={}, key={}", saved.getId(), saved.getSystemKey());

        Optional<LiftSystem> found = liftSystemRepository.findById(saved.getId());
        if (found.isPresent()) {
            logger.info("✓ Found LiftSystem by ID: {}", found.get().getSystemKey());
        } else {
            throw new AssertionError("Failed to find LiftSystem by ID");
        }

        Optional<LiftSystem> foundByKey = liftSystemRepository.findBySystemKey("demo-system");
        if (foundByKey.isPresent()) {
            logger.info("✓ Found LiftSystem by systemKey: {}", foundByKey.get().getDisplayName());
        } else {
            throw new AssertionError("Failed to find LiftSystem by systemKey");
        }

        saved.setDisplayName("Updated Demo System");
        LiftSystem updated = liftSystemRepository.save(saved);
        logger.info("✓ Updated LiftSystem: displayName={}", updated.getDisplayName());
    }

    private void verifyLiftSystemVersionOperations() {
        logger.info("--- Verifying LiftSystemVersion CRUD Operations ---");

        Optional<LiftSystem> system = liftSystemRepository.findBySystemKey("demo-system");
        if (!system.isPresent()) {
            throw new AssertionError("LiftSystem not found for version operations");
        }

        String configJson = "{\"minFloor\": 1, \"maxFloor\": 10, \"numLifts\": 2}";
        LiftSystemVersion version = new LiftSystemVersion(
                system.get(),
                1,
                configJson
        );

        LiftSystemVersion saved = versionRepository.save(version);
        logger.info("✓ Created LiftSystemVersion: id={}, version={}, status={}",
                saved.getId(), saved.getVersionNumber(), saved.getStatus());

        Optional<LiftSystemVersion> found = versionRepository.findById(saved.getId());
        if (found.isPresent()) {
            logger.info("✓ Found LiftSystemVersion by ID: version={}",
                    found.get().getVersionNumber());
        } else {
            throw new AssertionError("Failed to find LiftSystemVersion by ID");
        }
    }

    private void verifyJsonbMapping() {
        logger.info("--- Verifying JSONB Field Mapping ---");

        Optional<LiftSystem> system = liftSystemRepository.findBySystemKey("demo-system");
        if (!system.isPresent()) {
            throw new AssertionError("LiftSystem not found for JSONB verification");
        }

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

        LiftSystemVersion version = new LiftSystemVersion(system.get(), 2, complexJson);
        LiftSystemVersion saved = versionRepository.save(version);

        logger.info("✓ Saved complex JSONB config: id={}", saved.getId());

        Optional<LiftSystemVersion> retrieved = versionRepository.findById(saved.getId());
        if (retrieved.isPresent() && retrieved.get().getConfig().equals(complexJson)) {
            logger.info("✓ Retrieved JSONB config matches original");
        } else {
            throw new AssertionError("JSONB config mismatch after retrieval");
        }
    }

    private void verifyRelationships() {
        logger.info("--- Verifying Entity Relationships ---");

        Optional<LiftSystem> system = liftSystemRepository.findBySystemKey("demo-system");
        if (!system.isPresent()) {
            throw new AssertionError("LiftSystem not found for relationship verification");
        }

        List<LiftSystemVersion> versions = versionRepository
                .findByLiftSystemIdOrderByVersionNumberDesc(system.get().getId());

        if (versions.size() >= 2) {
            logger.info("✓ Found {} versions for LiftSystem", versions.size());
            logger.info("✓ Latest version number: {}", versions.get(0).getVersionNumber());
        } else {
            throw new AssertionError("Expected at least 2 versions");
        }
    }

    private void verifyQueryMethods() {
        logger.info("--- Verifying Custom Query Methods ---");

        Optional<LiftSystem> system = liftSystemRepository.findBySystemKey("demo-system");
        if (!system.isPresent()) {
            throw new AssertionError("LiftSystem not found for query verification");
        }

        Integer maxVersion = versionRepository
                .findMaxVersionNumberByLiftSystemId(system.get().getId());
        logger.info("✓ Max version number: {}", maxVersion);

        Optional<LiftSystemVersion> version = versionRepository
                .findByLiftSystemIdAndVersionNumber(system.get().getId(), 1);
        if (version.isPresent()) {
            version.get().publish();
            versionRepository.save(version.get());
            logger.info("✓ Published version 1");
        }

        List<LiftSystemVersion> publishedVersions = versionRepository
                .findByLiftSystemIdAndIsPublishedTrue(system.get().getId());
        logger.info("✓ Found {} published versions", publishedVersions.size());

        List<LiftSystemVersion> drafts = versionRepository.findByStatus(VersionStatus.DRAFT);
        logger.info("✓ Found {} draft versions in database", drafts.size());

        boolean exists = liftSystemRepository.existsBySystemKey("demo-system");
        logger.info("✓ existsBySystemKey returned: {}", exists);
    }
}
