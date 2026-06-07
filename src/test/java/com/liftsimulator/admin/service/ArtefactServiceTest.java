package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertTimeout;

class ArtefactServiceTest {

    private final ArtefactService artefactService = new ArtefactService(new ObjectMapper());

    @TempDir
    private Path tempDir;

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
    void readLogsTailsHundredThousandLineFileWithinMicrobenchmarkBudget() throws IOException {
        writeRunLog(100_000);
        SimulationRun run = runForTempDir();

        String logs = assertTimeout(Duration.ofSeconds(2), () -> artefactService.readLogs(run, 10_000));

        String[] tailedLines = logs.split("\\n");
        assertEquals(10_000, tailedLines.length);
        assertEquals("line-90001", tailedLines[0]);
        assertEquals("line-100000", tailedLines[tailedLines.length - 1]);
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
