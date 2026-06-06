package com.liftsimulator.admin.service;

import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying that a single artefact directory is created per run
 * and no orphaned directories are left behind (issue #377).
 */
public class SimulationRunDirectoryIntegrationTest extends LocalIntegrationTest {

    @TempDir
    static Path artefactsRoot;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("simulation.artefacts.base-path", () -> artefactsRoot.toString());
    }

    @Autowired
    private SimulationRunService runService;

    @Autowired
    private LiftSystemRepository liftSystemRepository;

    @Autowired
    private LiftSystemVersionRepository versionRepository;

    @Autowired
    private SimulationRunRepository runRepository;

    private LiftSystem testSystem;
    private LiftSystemVersion testVersion;

    private static final String VALID_CONFIG = "{\"minFloor\": 0, \"maxFloor\": 5, " +
            "\"travelTicksPerFloor\": 1, \"doorTransitionTicks\": 1, \"doorDwellTicks\": 2, " +
            "\"doorReopenWindowTicks\": 1, \"homeFloor\": 0, \"idleTimeoutTicks\": 3, " +
            "\"controllerStrategy\": \"NEAREST_REQUEST_ROUTING\", " +
            "\"idleParkingMode\": \"PARK_TO_HOME_FLOOR\"}";

    @BeforeEach
    void setUp() throws IOException {
        runRepository.deleteAll();
        versionRepository.deleteAll();
        liftSystemRepository.deleteAll();

        // Remove any run directories left by a previous test so each test starts clean
        try (var entries = Files.newDirectoryStream(artefactsRoot)) {
            for (Path entry : entries) {
                Files.walk(entry)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) { } });
            }
        }

        testSystem = liftSystemRepository.save(new LiftSystem("dir-test-system", "Dir Test System", ""));
        testVersion = new LiftSystemVersion(testSystem, 1, VALID_CONFIG);
        testVersion.setStatus(VersionStatus.PUBLISHED);
        testVersion = versionRepository.save(testVersion);
    }

    @Test
    void createAndStartRun_createsExactlyOneDirectory() throws IOException {
        SimulationRun run = runService.createAndStartRun(testSystem.getId(), testVersion.getId(), null, 1L);

        // The persisted path must be inside artefactsRoot
        Path runDir = Path.of(run.getArtefactBasePath());
        assertTrue(runDir.startsWith(artefactsRoot),
                "artefactBasePath should be under the configured base path");
        assertTrue(Files.isDirectory(runDir),
                "artefact directory must exist on disk");

        // Exactly one subdirectory should exist under artefactsRoot (no orphans)
        List<Path> dirs = Files.walk(artefactsRoot, 1)
                .filter(p -> !p.equals(artefactsRoot))
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
        assertEquals(1, dirs.size(),
                "Exactly one run directory should exist; found: " + dirs);
        assertEquals(runDir.toAbsolutePath(), dirs.get(0).toAbsolutePath(),
                "The directory on disk must match the persisted artefactBasePath");
    }

    @Test
    void createAndStartRun_directoryNameMatchesRunId() throws IOException {
        SimulationRun run = runService.createAndStartRun(testSystem.getId(), testVersion.getId(), null, 2L);

        Path runDir = Path.of(run.getArtefactBasePath());
        assertEquals("run-" + run.getId(), runDir.getFileName().toString(),
                "Run directory name must be run-{id}");
    }
}
