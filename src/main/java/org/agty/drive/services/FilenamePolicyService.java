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
