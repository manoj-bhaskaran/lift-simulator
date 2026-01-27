package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.ArtefactInfo;
import com.liftsimulator.admin.entity.SimulationRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for managing simulation run artefacts with security controls.
 */
@Service
public class ArtefactService {

    private static final Logger logger = LoggerFactory.getLogger(ArtefactService.class);
    private static final int DEFAULT_TAIL_LINES = 100;
    private static final int MAX_TAIL_LINES = 10000;

    private final ObjectMapper objectMapper;

    public ArtefactService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

    public record ArtefactDownload(
        Path path,
        String fileName,
        long size,
        String mimeType
    ) {
    }

    /**
     * Lists all artefacts in a simulation run's artefact directory.
     *
     * @param run the simulation run
     * @return list of artefact information
     * @throws IOException if directory access fails
     * @throws IllegalStateException if artefact base path is not set
     */
    public List<ArtefactInfo> listArtefacts(SimulationRun run) throws IOException {
        String basePath = run.getArtefactBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("Artefact base path is not set for run " + run.getId());
        }

        Path directory = validateAndResolvePath(basePath, null);

        if (!Files.exists(directory)) {
            return Collections.emptyList();
        }

        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("Artefact base path is not a directory: " + basePath);
        }

        List<ArtefactInfo> artefacts = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Path fileNamePath = path.getFileName();
                        if (fileNamePath == null) {
                            logger.warn("Skipping file with null name: " + path);
                            return;
                        }
                        String relativePath = directory.relativize(path).toString();
                        String fileName = fileNamePath.toString();
                        long size = Files.size(path);
                        artefacts.add(ArtefactInfo.of(fileName, relativePath, size));
                    } catch (IOException e) {
                        logger.warn("Failed to read file info for: " + path, e);
                    }
                });
        }

        return artefacts;
    }

    /**
     * Resolves an artefact file for download.
     *
     * @param run the simulation run
     * @param relativePath the relative path within the artefact directory
     * @return artefact download metadata
     * @throws IOException if file metadata cannot be read
     */
    public ArtefactDownload getArtefact(SimulationRun run, String relativePath) throws IOException {
        String basePath = run.getArtefactBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("Artefact base path is not set for run " + run.getId());
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Artefact path must be provided");
        }

        Path artefactPath;
        try {
            artefactPath = validateAndResolvePath(basePath, relativePath);
        } catch (SecurityException e) {
            throw new IllegalArgumentException("Invalid artefact path: " + relativePath, e);
        }

        if (!Files.exists(artefactPath) || Files.isDirectory(artefactPath)) {
            throw new ResourceNotFoundException("Artefact not found: " + relativePath);
        }

        String fileName = artefactPath.getFileName() != null
            ? artefactPath.getFileName().toString()
            : relativePath;
        long size = Files.size(artefactPath);
        String mimeType = Files.probeContentType(artefactPath);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        return new ArtefactDownload(artefactPath, fileName, size, mimeType);
    }

    /**
     * Reads logs from a simulation run with optional tail functionality.
     *
     * @param run the simulation run
     * @param tail number of lines to read from the end (null for all)
     * @return the log content
     * @throws IOException if log file access fails
     */
    public String readLogs(SimulationRun run, Integer tail) throws IOException {
        String basePath = run.getArtefactBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IOException("Artefact base path is not set for run " + run.getId());
        }

        // Try common log file names
        String[] logFileNames = {"simulation.log", "output.log", "run.log"};
        Path logPath = null;

        for (String logFileName : logFileNames) {
            Path candidatePath = validateAndResolvePath(basePath, logFileName);
            if (Files.exists(candidatePath)) {
                logPath = candidatePath;
                break;
            }
        }

        if (logPath == null || !Files.exists(logPath)) {
            return "No log file found for simulation run " + run.getId();
        }

        // Validate tail parameter
        int linesToRead = tail != null ? Math.min(tail, MAX_TAIL_LINES) : -1;

        if (linesToRead < 0) {
            // Read entire file
            return Files.readString(logPath);
        } else {
            // Read last N lines
            return readLastNLines(logPath, linesToRead);
        }
    }

    /**
     * Reads the results JSON from a simulation run.
     *
     * @param run the simulation run
     * @return the results as JSON
     * @throws IOException if results file access fails
     */
    public JsonNode readResults(SimulationRun run) throws IOException {
        String basePath = run.getArtefactBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("Artefact base path is not set for run " + run.getId());
        }

        // Try common result file names
        String[] resultFileNames = {"results.json", "output.json", "simulation-results.json"};
        Path resultsPath = null;

        for (String resultFileName : resultFileNames) {
            Path candidatePath = validateAndResolvePath(basePath, resultFileName);
            if (Files.exists(candidatePath)) {
                resultsPath = candidatePath;
                break;
            }
        }

        if (resultsPath == null || !Files.exists(resultsPath)) {
            throw new IOException("No results file found for simulation run " + run.getId());
        }

        return objectMapper.readTree(resultsPath.toFile());
    }

    /**
     * Validates and resolves a path, preventing path traversal attacks.
     *
     * @param basePath the base directory path
     * @param relativePath the relative path within the base directory (can be null)
     * @return the validated absolute path
     * @throws SecurityException if path traversal is detected
     */
    private Path validateAndResolvePath(String basePath, String relativePath) {
        try {
            Path base = Paths.get(basePath).toAbsolutePath().normalize();
            Path resolved;

            if (relativePath == null || relativePath.isBlank()) {
                resolved = base;
            } else {
                // Normalize the relative path to prevent traversal
                Path relative = Paths.get(relativePath).normalize();

                // Check for absolute path or path traversal attempts
                if (relative.isAbsolute() || relative.toString().contains("..")) {
                    throw new SecurityException("Path traversal attempt detected: " + relativePath);
                }

                resolved = base.resolve(relative).normalize();
            }

            // Ensure the resolved path is still within the base directory
            if (!resolved.startsWith(base)) {
                throw new SecurityException("Path traversal attempt detected: resolved path "
                        + resolved + " is outside base directory " + base);
            }

            return resolved;
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                throw (SecurityException) e;
            }
            throw new SecurityException("Invalid path: " + basePath
                    + (relativePath != null ? "/" + relativePath : ""), e);
        }
    }

    /**
     * Reads the last N lines from a file efficiently.
     *
     * @param path the file path
     * @param n the number of lines to read
     * @return the last N lines as a string
     * @throws IOException if file reading fails
     */
    private String readLastNLines(Path path, int n) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > n) {
                    lines.remove(0); // Remove oldest line to maintain size
                }
            }
        }

        return String.join("\n", lines);
    }
}
