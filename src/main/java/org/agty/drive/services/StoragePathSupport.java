package org.agty.drive.services;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

public final class StoragePathSupport {

    private StoragePathSupport() {
    }

    public static Path resolveRootPath(String contentDir) {
        Path configuredPath = Paths.get(contentDir == null || contentDir.isBlank() ? "content" : contentDir.trim());
        return configuredPath.isAbsolute()
                ? configuredPath.normalize()
                : Paths.get(System.getProperty("user.dir")).resolve(configuredPath).normalize();
    }

    public static String buildStorageName(String checksumSha256, String extension, LocalDate date) {
        String checksum = checksumSha256 == null || checksumSha256.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : checksumSha256.toLowerCase();

        LocalDate actualDate = date == null ? LocalDate.now() : date;
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String suffix = extension == null || extension.isBlank() ? "" : "." + extension.toLowerCase();

        return "%s/%s/%s/%s/%s/%s%s".formatted(
                actualDate.getYear(),
                leftPad(actualDate.getMonthValue()),
                leftPad(actualDate.getDayOfMonth()),
                checksum.substring(0, 2),
                checksum.substring(2, 4),
                fileId,
                suffix
        );
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
        if (storageName == null || storageName.isBlank()) {
            throw new IllegalArgumentException("storageName is required");
        }

        String normalizedName = storageName.replace("\\", "/").trim();
        int slashIndex = normalizedName.lastIndexOf('/');
        String directory = slashIndex >= 0 ? normalizedName.substring(0, slashIndex + 1) : "";
        String filename = slashIndex >= 0 ? normalizedName.substring(slashIndex + 1) : normalizedName;
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? filename.substring(0, dotIndex) : filename;
        return directory + baseName + ".thumb.png";
    }

    private static String leftPad(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
