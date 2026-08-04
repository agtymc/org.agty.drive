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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.HexFormat;

public final class StoragePathSupport {

    private StoragePathSupport() {
    }

    public static Path resolveRootPath(String contentDir) {
        Path configuredPath = Paths.get(contentDir == null || contentDir.isBlank() ? "content" : contentDir.trim());
        return configuredPath.isAbsolute()
                ? configuredPath.normalize()
                : Paths.get(System.getProperty("user.dir")).resolve(configuredPath).normalize();
    }

    public static String buildStorageName(String storageKey, String extension, LocalDate date) {
        String normalizedKey = storageKey == null || storageKey.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : storageKey.toLowerCase();
        LocalDate actualDate = date == null ? LocalDate.now() : date;
        String suffix = extension == null || extension.isBlank() ? "" : "." + extension.toLowerCase();

        return "%s/%s/%s/%s/%s/%s%s".formatted(
                actualDate.getYear(),
                leftPad(actualDate.getMonthValue()),
                leftPad(actualDate.getDayOfMonth()),
                normalizedKey.substring(0, 2),
                normalizedKey.substring(2, 4),
                normalizedKey,
                suffix
        );
    }

    public static String buildStorageKey(Long ownerId, String originalFilename, LocalDate date) {
        LocalDate actualDate = date == null ? LocalDate.now() : date;
        String source = "%s:%s:%s:%s".formatted(
                ownerId == null ? "0" : ownerId,
                actualDate,
                UUID.randomUUID(),
                originalFilename == null ? "" : originalFilename.trim()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static Path resolveContentPath(Path rootPath, String storageName) {
        String normalizedName = storageName.replace("\\", "/");
        while (normalizedName.startsWith("/")) {
            normalizedName = normalizedName.substring(1);
        }

        Path resolved = rootPath.resolve(normalizedName).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new IllegalArgumentException("Invalid storageName path: " + storageName);
        }
        return resolved;
    }

    public static String buildThumbnailStorageName(String storageName) {
        return buildThumbnailStorageName(storageName, ".thumb.jpg");
    }

    public static String buildLegacyThumbnailStorageName(String storageName) {
        return buildThumbnailStorageName(storageName, ".thumb.png");
    }

    private static String buildThumbnailStorageName(String storageName, String suffix) {
        if (storageName == null || storageName.isBlank()) {
            throw new IllegalArgumentException("storageName is required");
        }

        String normalizedName = storageName.replace("\\", "/").trim();
        int slashIndex = normalizedName.lastIndexOf('/');
        String directory = slashIndex >= 0 ? normalizedName.substring(0, slashIndex + 1) : "";
        String filename = slashIndex >= 0 ? normalizedName.substring(slashIndex + 1) : normalizedName;
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? filename.substring(0, dotIndex) : filename;
        return directory + baseName + suffix;
    }

    private static String leftPad(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
