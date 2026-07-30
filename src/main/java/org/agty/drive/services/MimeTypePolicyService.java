package org.agty.drive.services;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MimeTypePolicyService {

    private static final String DEFAULT_MIME_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
            "application/javascript",
            "application/x-javascript",
            "application/xhtml+xml",
            "image/svg+xml",
            "text/html",
            "text/javascript"
    );

    private static final Map<String, String> EXTENSION_MIME_TYPES = Map.ofEntries(
            Map.entry("aac", "audio/aac"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("csv", "text/csv"),
            Map.entry("flac", "audio/flac"),
            Map.entry("gif", "image/gif"),
            Map.entry("gz", "application/gzip"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("json", "application/json"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("md", "text/markdown"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("txt", "text/plain"),
            Map.entry("wav", "audio/wav"),
            Map.entry("webm", "video/webm"),
            Map.entry("webp", "image/webp"),
            Map.entry("xml", "application/xml"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("yml", "application/yaml"),
            Map.entry("yaml", "application/yaml"),
            Map.entry("zip", "application/zip")
    );

    public String normalizeUploadedMimeType(String mimeType, String extension) {
        String normalizedMimeType = normalizeMimeType(mimeType);
        if (normalizedMimeType != null && !BLOCKED_MIME_TYPES.contains(normalizedMimeType)) {
            return normalizedMimeType;
        }

        String normalizedExtension = normalizeExtension(extension);
        if (normalizedExtension != null) {
            return EXTENSION_MIME_TYPES.getOrDefault(normalizedExtension, DEFAULT_MIME_TYPE);
        }

        return DEFAULT_MIME_TYPE;
    }

    public MediaType resolveResponseMediaType(String mimeType) {
        String normalizedMimeType = normalizeMimeType(mimeType);
        if (normalizedMimeType == null || BLOCKED_MIME_TYPES.contains(normalizedMimeType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(normalizedMimeType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }

        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }

        int separatorIndex = normalized.indexOf(';');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(0, separatorIndex).trim();
        }

        if (!normalized.contains("/")) {
            return null;
        }

        return normalized;
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            return null;
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
