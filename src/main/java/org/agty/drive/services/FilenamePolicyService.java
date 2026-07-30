package org.agty.drive.services;

import org.springframework.stereotype.Service;

@Service
public class FilenamePolicyService {

    private static final int MAX_FILENAME_LENGTH = 255;

    public String normalizeFilename(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value
                .replace('\\', '_')
                .replace('/', '_')
                .replaceAll("[\\p{Cntrl}]+", " ")
                .trim();
        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("_+", "_");

        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        if (normalized.isBlank()
                || ".".equals(normalized)
                || "..".equals(normalized)
                || normalized.replace("_", "").isBlank()) {
            return null;
        }

        if (normalized.length() > MAX_FILENAME_LENGTH) {
            normalized = normalized.substring(0, MAX_FILENAME_LENGTH).trim();
        }

        return normalized.isBlank() ? null : normalized;
    }

    public String normalizeArchiveEntryName(String value, String fallback) {
        String normalized = normalizeFilename(value);
        return normalized == null ? fallback : normalized;
    }
}
