package org.agty.drive.services;

import org.agty.utils.AgtyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileContentStorageService {

    private final Path rootPath;

    public FileContentStorageService(@Value("${storage.content_dir:content}") String contentDir) {
        this.rootPath = StoragePathSupport.resolveRootPath(contentDir);
        ensureRootExists();
    }

    public void save(String storageName, byte[] content) {
        if (storageName == null || storageName.isBlank()) {
            throw new IllegalArgumentException("storageName is required");
        }

        try {
            Path path = resolve(storageName);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, content == null ? new byte[0] : content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file content: " + storageName, e);
        }
    }

    public Path createTempFile() {
        try {
            Path tempDir = rootPath.resolve(".upload-tmp");
            Files.createDirectories(tempDir);
            return Files.createTempFile(tempDir, "upload-", ".tmp");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp upload file", e);
        }
    }

    public void moveIntoStorage(Path tempPath, String storageName) {
        if (tempPath == null) {
            throw new IllegalArgumentException("tempPath is required");
        }
        if (storageName == null || storageName.isBlank()) {
            throw new IllegalArgumentException("storageName is required");
        }

        try {
            Path path = resolve(storageName);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to move temp upload into storage: " + storageName, e);
        }
    }

    public byte[] read(String storageName) {
        if (storageName == null || storageName.isBlank()) {
            return null;
        }

        Path path = resolve(storageName);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return null;
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content: " + storageName, e);
        }
    }

    public Path resolveExistingPath(String storageName) {
        if (storageName == null || storageName.isBlank()) {
            return null;
        }
        Path path = resolve(storageName);
        return Files.exists(path) && Files.isRegularFile(path) ? path : null;
    }

    public InputStream openStream(String storageName) {
        Path path = resolveExistingPath(storageName);
        if (path == null) {
            return null;
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open file content stream: " + storageName, e);
        }
    }

    public long size(String storageName) {
        Path path = resolveExistingPath(storageName);
        if (path == null) {
            return -1L;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content size: " + storageName, e);
        }
    }

    public void delete(String storageName) {
        if (storageName == null || storageName.isBlank()) {
            return;
        }

        Path path = resolve(storageName);
        try {
            Files.deleteIfExists(path);
            cleanupEmptyParents(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file content: " + storageName, e);
        }
    }

    public void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
            cleanupEmptyParents(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete temp file: " + path, e);
        }
    }

    public String getUsableSpaceTitle() {
        try {
            FileStore fileStore = Files.getFileStore(rootPath);
            return AgtyUtils.filesizeToTitle(fileStore.getUsableSpace(), "ru");
        } catch (IOException e) {
            return "—";
        }
    }

    public String getTotalSpaceTitle() {
        try {
            FileStore fileStore = Files.getFileStore(rootPath);
            return AgtyUtils.filesizeToTitle(fileStore.getTotalSpace(), "ru");
        } catch (IOException e) {
            return "—";
        }
    }

    private Path resolve(String storageName) {
        return StoragePathSupport.resolveContentPath(rootPath, storageName);
    }

    private void cleanupEmptyParents(Path path) throws IOException {
        Path current = path;
        while (current != null && !current.equals(rootPath) && current.startsWith(rootPath)) {
            if (!Files.exists(current) || !Files.isDirectory(current)) {
                current = current.getParent();
                continue;
            }
            try (var stream = Files.list(current)) {
                if (stream.findAny().isPresent()) {
                    break;
                }
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private void ensureRootExists() {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + rootPath, e);
        }
    }
}
