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
        ensureStagingExists();
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

    public Path createStagingFile(String extension) {
        try {
            Path stagingDir = stagingDir();
            Files.createDirectories(stagingDir);
            String suffix = extension == null || extension.isBlank() ? ".tmp" : "." + extension.toLowerCase();
            return Files.createTempFile(stagingDir, "upload-", suffix);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create staging upload file", e);
        }
    }

    public void moveIntoStorage(Path stagingPath, String storageName) {
        if (stagingPath == null) {
            throw new IllegalArgumentException("stagingPath is required");
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
            Files.move(stagingPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to move staging upload into storage: " + storageName, e);
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

    public void deleteStagingFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
            cleanupEmptyParents(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete staging file: " + path, e);
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

    private Path stagingDir() {
        return rootPath.resolve(".upload-staging");
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

    private void ensureStagingExists() {
        try {
            Files.createDirectories(stagingDir());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create staging directory: " + stagingDir(), e);
        }
    }
}
