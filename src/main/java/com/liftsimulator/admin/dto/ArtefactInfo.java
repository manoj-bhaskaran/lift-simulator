package com.liftsimulator.admin.dto;

/**
 * DTO representing an artefact file associated with a simulation run.
 */
public record ArtefactInfo(
    String name,
    String path,
    Long size,
    String mimeType
) {
    /**
     * Creates an artefact info with basic details.
     *
     * @param name the file name
     * @param path the relative path within the artefact directory
     * @param size the file size in bytes
     * @return the artefact info
     */
    public static ArtefactInfo of(String name, String path, Long size) {
        return new ArtefactInfo(name, path, size, determineMimeType(name));
    }

    /**
     * Determines MIME type based on file extension.
     *
     * @param fileName the file name
     * @return the MIME type
     */
    private static String determineMimeType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".json")) {
            return "application/json";
        } else if (lowerName.endsWith(".txt") || lowerName.endsWith(".log")) {
            return "text/plain";
        } else if (lowerName.endsWith(".csv")) {
            return "text/csv";
        } else if (lowerName.endsWith(".scenario")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }
}
