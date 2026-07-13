package com.liftsimulator.admin.config;

/**
 * Shared helper for validating configured secrets (API keys, passwords).
 *
 * <p>Rejects values that are missing, blank, or left at the well-known
 * {@code CHANGE_ME} placeholder so insecure deployments fail fast at startup.
 */
final class SecuritySecrets {

    private static final String PLACEHOLDER_SECRET = "CHANGE_ME";

    private SecuritySecrets() {
    }

    /**
     * Returns {@code true} when the secret is unusable: {@code null}, blank, or
     * equal (case-insensitively) to the {@code CHANGE_ME} placeholder.
     */
    static boolean isMissingOrPlaceholder(String secret) {
        return secret == null || secret.isBlank() || PLACEHOLDER_SECRET.equalsIgnoreCase(secret.trim());
    }
}
