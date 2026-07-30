/*
 * Copyright 2026 Vladimir V
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
