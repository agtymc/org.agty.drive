package org.agty.drive.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FilenamePolicyServiceTest {

    private final FilenamePolicyService filenamePolicyService = new FilenamePolicyService();

    @Test
    void shouldNormalizeUnsafeFilenameCharacters() {
        String normalized = filenamePolicyService.normalizeFilename("  ..\\\\report/\n2026.txt  ");
        assertEquals("_report_ 2026.txt", normalized);
    }

    @Test
    void shouldRejectBlankOrDotOnlyFilename() {
        assertNull(filenamePolicyService.normalizeFilename("   "));
        assertNull(filenamePolicyService.normalizeFilename("..."));
    }

    @Test
    void shouldFallbackArchiveEntryName() {
        assertEquals("file", filenamePolicyService.normalizeArchiveEntryName("../", "file"));
    }
}
