package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.ArtefactInfo;
import com.liftsimulator.admin.entity.SimulationRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtefactServiceTest {

    private final ArtefactService artefactService = new ArtefactService(new ObjectMapper());

    @TempDir
    private Path tempDir;

    @Test
    void listArtefactsReturnsRelativePathsForNestedFiles() throws IOException {
        Files.createDirectories(tempDir.resolve("metrics"));
        Files.writeString(tempDir.resolve("results.json"), "{\"status\":\"SUCCEEDED\"}");
        Files.writeString(tempDir.resolve("metrics/per-floor.csv"), "floor,requests\n0,1\n");

        List<ArtefactInfo> artefacts = artefactService.listArtefacts(runForTempDir());

        assertEquals(2, artefacts.size());
        assertTrue(artefacts.stream().anyMatch(artefact -> artefact.name().equals("results.json")
            && artefact.path().equals("results.json")
            && artefact.mimeType().equals("application/json")));
        assertTrue(artefacts.stream().anyMatch(artefact -> artefact.name().equals("per-floor.csv")
            && artefact.path().equals("metrics/per-floor.csv")
            && artefact.mimeType().equals("text/csv")));
    }

    @Test
    void listArtefactsReturnsEmptyListForMissingDirectory() throws IOException {
        SimulationRun run = new SimulationRun();
        run.setId(1L);
        run.setArtefactBasePath(tempDir.resolve("missing").toString());

        assertTrue(artefactService.listArtefacts(run).isEmpty());
    }

    @Test
    void getArtefactReturnsDownloadMetadataForSafeRelativePath() throws IOException {
        Files.createDirectories(tempDir.resolve("outputs"));
        Path resultFile = tempDir.resolve("outputs/results.json");
        Files.writeString(resultFile, "{\"ok\":true}");

        ArtefactService.ArtefactDownload download = artefactService.getArtefact(
            runForTempDir(),
            "outputs/results.json"
        );

        assertEquals(resultFile.toAbsolutePath().normalize(), download.path());
        assertEquals("results.json", download.fileName());
        assertEquals(Files.size(resultFile), download.size());
        assertTrue(download.mimeType().equals("application/json")
            || download.mimeType().equals("application/octet-stream"));
    }

    @Test
    void getArtefactRejectsPathTraversalOutsideArtefactDirectory() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> artefactService.getArtefact(runForTempDir(), "../secrets.txt")
        );

        assertTrue(exception.getMessage().contains("Invalid artefact path"));
    }

    @Test
    void getArtefactRejectsAbsolutePaths() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> artefactService.getArtefact(runForTempDir(), tempDir.resolve("results.json").toString())
        );

        assertTrue(exception.getMessage().contains("Invalid artefact path"));
    }

    @Test
    void getArtefactRejectsEscapedSymbolicLinks() throws IOException {
        Path externalFile = Files.createTempFile("lift-simulator-external", ".txt");
        Files.writeString(externalFile, "outside artefact root");
        Path link = tempDir.resolve("external-link.txt");
        Files.createSymbolicLink(link, externalFile);

        try {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> artefactService.getArtefact(runForTempDir(), "external-link.txt")
            );
            assertTrue(exception.getMessage().contains("Invalid artefact path"));
        } finally {
            Files.deleteIfExists(externalFile);
        }
    }

    @Test
    void getArtefactThrowsNotFoundForMissingFile() {
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> artefactService.getArtefact(runForTempDir(), "missing-results.json")
        );

        assertEquals("Artefact not found: missing-results.json", exception.getMessage());
    }

    @Test
    void readResultsParsesFirstKnownResultsFile() throws IOException {
        Files.writeString(tempDir.resolve("results.json"), "{\"runSummary\":{\"status\":\"SUCCEEDED\"}}");

        JsonNode results = artefactService.readResults(runForTempDir());

        assertEquals("SUCCEEDED", results.at("/runSummary/status").asText());
    }

    @Test
    void readResultsRejectsOversizedResultsFile() throws IOException {
        Files.writeString(tempDir.resolve("results.json"), "{\"data\":\"" + "x".repeat(1_048_577) + "\"}");

        IOException exception = assertThrows(
            IOException.class,
            () -> artefactService.readResults(runForTempDir())
        );

        assertTrue(exception.getMessage().contains("Results file exceeds"));
    }

    @Test
    void readResultsThrowsWhenResultsFileIsMissing() {
        IOException exception = assertThrows(
            IOException.class,
            () -> artefactService.readResults(runForTempDir())
        );

        assertTrue(exception.getMessage().contains("No results file found"));
    }

    @Test
    void readLogsReturnsMessageWhenLogFileIsMissing() throws IOException {
        String logs = artefactService.readLogs(runForTempDir(), 25);

        assertEquals("No log file found for simulation run 1", logs);
    }

    @Test
    void readLogsRejectsOversizedFullLogRead() throws IOException {
        Files.writeString(tempDir.resolve("simulation.log"), "x".repeat(1_048_577));

        IOException exception = assertThrows(
            IOException.class,
            () -> artefactService.readLogs(runForTempDir(), null)
        );

        assertTrue(exception.getMessage().contains("Log file exceeds"));
    }

    @Test
    void readLogsReturnsLastLinesFromLargeFile() throws IOException {
        writeRunLog(12_000);
        SimulationRun run = runForTempDir();

        String logs = artefactService.readLogs(run, 5);

        assertEquals("line-11996\nline-11997\nline-11998\nline-11999\nline-12000", logs);
    }

    @Test
    void readLogsHandlesZeroTailWithoutReadingLines() throws IOException {
        writeRunLog(10);
        SimulationRun run = runForTempDir();

        assertEquals("", artefactService.readLogs(run, 0));
    }

    @Test
    void readLogsCapsTailAtMaximumLineCount() throws IOException {
        writeRunLog(12_000);

        String logs = artefactService.readLogs(runForTempDir(), 20_000);

        assertEquals(10_000, logs.split("\\n").length);
        assertFalse(logs.contains("line-1999\n"));
        assertTrue(logs.startsWith("line-2001\n"));
    }

    @Test
    void readLogsTailsHundredThousandLineFileWithinMicrobenchmarkBudget() throws IOException {
        writeRunLog(100_000);
        SimulationRun run = runForTempDir();

        String logs = assertTimeout(Duration.ofSeconds(2), () -> artefactService.readLogs(run, 10_000));

        String[] tailedLines = logs.split("\\n");
        assertEquals(10_000, tailedLines.length);
        assertEquals("line-90001", tailedLines[0]);
        assertEquals("line-100000", tailedLines[tailedLines.length - 1]);
    }

    @Test
    void deleteArtefactsRemovesDirectoryAndNestedFiles() throws IOException {
        Files.createDirectories(tempDir.resolve("metrics"));
        Files.writeString(tempDir.resolve("results.json"), "{\"status\":\"SUCCEEDED\"}");
        Files.writeString(tempDir.resolve("metrics/per-floor.csv"), "floor,requests\n0,1\n");

        artefactService.deleteArtefacts(runForTempDir());

        assertFalse(Files.exists(tempDir));
    }

    @Test
    void deleteArtefactsIsNoOpForMissingDirectory() throws IOException {
        SimulationRun run = new SimulationRun();
        run.setId(1L);
        run.setArtefactBasePath(tempDir.resolve("missing").toString());

        // Should not throw even though the directory does not exist.
        artefactService.deleteArtefacts(run);
    }

    @Test
    void deleteArtefactsIsNoOpForBlankBasePath() throws IOException {
        SimulationRun run = new SimulationRun();
        run.setId(1L);
        run.setArtefactBasePath("");

        artefactService.deleteArtefacts(run);
    }

    private void writeRunLog(int lines) throws IOException {
        List<String> content = new ArrayList<>(lines);
        for (int line = 1; line <= lines; line++) {
            content.add("line-" + line);
        }
        Files.write(tempDir.resolve("simulation.log"), content);
    }

    private SimulationRun runForTempDir() {
        SimulationRun run = new SimulationRun();
        run.setId(1L);
        run.setArtefactBasePath(tempDir.toString());
        return run;
    }
}
