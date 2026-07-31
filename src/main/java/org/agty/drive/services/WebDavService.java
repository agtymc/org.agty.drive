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

import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class WebDavService {

    private final FolderService folderService;
    private final FileService fileService;
    private final FolderDeleteService folderDeleteService;
    private final FilenamePolicyService filenamePolicyService;

    public WebDavService(FolderService folderService,
                         FileService fileService,
                         FolderDeleteService folderDeleteService,
                         FilenamePolicyService filenamePolicyService) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.folderDeleteService = folderDeleteService;
        this.filenamePolicyService = filenamePolicyService;
    }

    public DavResource resolve(Long ownerId, String path) {
        return resolve(ownerId, path, null);
    }

    public DavResource resolve(Long ownerId, String path, Long scopeRootFolderId) {
        String normalizedPath = normalizePath(path);
        if ("/".equals(normalizedPath)) {
            if (scopeRootFolderId == null) {
                return DavResource.root();
            }
            FolderDto rootFolder = folderService.findByIdAndOwnerId(scopeRootFolderId, ownerId);
            return rootFolder == null ? null : DavResource.scopedRoot(rootFolder);
        }

        List<String> segments = splitPath(normalizedPath);
        Long parentId = scopeRootFolderId;
        boolean trailingSlash = normalizedPath.endsWith("/");

        for (int index = 0; index < segments.size(); index++) {
            String segment = segments.get(index);
            boolean last = index == segments.size() - 1;
            FolderDto folder = findFolderChild(ownerId, parentId, segment);
            FileItemDto file = last ? findFileChild(ownerId, parentId, segment) : null;

            if (!last) {
                if (folder == null) {
                    return null;
                }
                parentId = folder.getId();
                continue;
            }

            if (trailingSlash) {
                return folder == null ? null : DavResource.folder(folder);
            }
            if (file != null) {
                return DavResource.file(file);
            }
            return folder == null ? null : DavResource.folder(folder);
        }

        return null;
    }

    public List<DavResource> listChildren(Long ownerId, DavResource resource) {
        return listChildren(ownerId, resource, null);
    }

    public List<DavResource> listChildren(Long ownerId, DavResource resource, Long scopeRootFolderId) {
        Long parentId;
        if (resource == null) {
            parentId = scopeRootFolderId;
        } else if (resource.isRoot()) {
            parentId = resource.folder() != null ? resource.folder().getId() : scopeRootFolderId;
        } else {
            parentId = resource.folder().getId();
        }
        List<DavResource> result = new ArrayList<>();
        for (FolderDto folder : folderService.findByOwnerIdAndParentId(ownerId, parentId)) {
            result.add(DavResource.folder(folder));
        }
        for (FileItemDto file : fileService.findByOwnerIdAndFolderId(ownerId, parentId)) {
            result.add(DavResource.file(file));
        }
        result.sort(Comparator
                .comparing(DavResource::isCollection, Comparator.reverseOrder())
                .thenComparing(resourceItem -> normalizedName(resourceItem.name()))
                .thenComparing(resourceItem -> resourceItem.id() == null ? Long.MAX_VALUE : resourceItem.id()));
        return result;
    }

    public DavOperationResult createFolder(Long ownerId, String path) {
        return createFolder(ownerId, path, null);
    }

    public DavOperationResult createFolder(Long ownerId, String path, Long scopeRootFolderId) {
        DavTarget target = resolveTarget(ownerId, stripTrailingSlash(path), scopeRootFolderId);
        if (!target.valid()) {
            return DavOperationResult.conflict("Некорректный путь.");
        }
        if (target.existing() != null) {
            return DavOperationResult.methodNotAllowed("Ресурс уже существует.");
        }
        if (!target.parentExists()) {
            return DavOperationResult.conflict("Родительская папка не найдена.");
        }
        if (findFileChild(ownerId, target.parentId(), target.name()) != null) {
            return DavOperationResult.conflict("В папке уже есть файл с таким именем.");
        }
        if (folderService.existsByOwnerIdAndParentIdAndName(ownerId, target.parentId(), target.name())) {
            return DavOperationResult.conflict("В папке уже есть папка с таким именем.");
        }

        FolderDto folderDto = new FolderDto();
        folderDto.setOwnerId(ownerId);
        folderDto.setParentId(target.parentId());
        folderDto.setName(target.name());
        folderDto.setPathKey(folderService.buildPathKeyForCreate(ownerId, target.parentId(), target.name()));
        folderDto.setSortOrder(0);
        FolderDto saved = folderService.save(folderDto);
        return saved == null
                ? DavOperationResult.serverError("Не удалось создать папку.")
                : DavOperationResult.created(saved);
    }

    public DavOperationResult putFile(Long ownerId, String path, String contentType, InputStream inputStream) {
        return putFile(ownerId, path, contentType, inputStream, null);
    }

    public DavOperationResult putFile(Long ownerId, String path, String contentType, InputStream inputStream, Long scopeRootFolderId) {
        DavTarget target = resolveTarget(ownerId, path, scopeRootFolderId);
        if (!target.valid()) {
            return DavOperationResult.conflict("Некорректный путь.");
        }
        if (!target.parentExists()) {
            return DavOperationResult.conflict("Родительская папка не найдена.");
        }
        if (target.existing() != null && target.existing().isCollection()) {
            return DavOperationResult.conflict("Нельзя записать файл поверх папки.");
        }
        if (findFolderChild(ownerId, target.parentId(), target.name()) != null) {
            return DavOperationResult.conflict("В папке уже есть папка с таким именем.");
        }

        FileItemDto saved = fileService.uploadStream(ownerId, target.parentId(), target.name(), contentType, inputStream, true);
        if (saved == null) {
            return DavOperationResult.conflict("Не удалось сохранить файл.");
        }
        return target.existing() == null
                ? DavOperationResult.created(saved)
                : DavOperationResult.updated(saved);
    }

    public DavOperationResult delete(Long ownerId, String path) {
        return delete(ownerId, path, null);
    }

    public DavOperationResult delete(Long ownerId, String path, Long scopeRootFolderId) {
        DavResource resource = resolve(ownerId, path, scopeRootFolderId);
        if (resource == null) {
            return DavOperationResult.notFound("Ресурс не найден.");
        }
        if (resource.isRoot() || resource.isScopedRoot()) {
            return DavOperationResult.methodNotAllowed("Корень диска удалить нельзя.");
        }

        String error = resource.isCollection()
                ? folderDeleteService.deleteByIdAndOwnerId(resource.folder().getId(), ownerId)
                : fileService.deleteByIdAndOwnerId(resource.file().getId(), ownerId);
        return error == null ? DavOperationResult.noContent() : DavOperationResult.serverError(error);
    }

    public DavOperationResult move(Long ownerId, String sourcePath, String destinationPath, boolean overwrite) {
        return move(ownerId, sourcePath, destinationPath, overwrite, null);
    }

    public DavOperationResult move(Long ownerId, String sourcePath, String destinationPath, boolean overwrite, Long scopeRootFolderId) {
        DavResource source = resolve(ownerId, sourcePath, scopeRootFolderId);
        if (source == null) {
            return DavOperationResult.notFound("Исходный ресурс не найден.");
        }
        if (source.isRoot() || source.isScopedRoot()) {
            return DavOperationResult.methodNotAllowed("Корень диска перемещать нельзя.");
        }

        String normalizedSourcePath = normalizePath(sourcePath);
        String normalizedDestinationPath = stripTrailingSlash(destinationPath);
        if (normalizedSourcePath.equals(normalizedDestinationPath)) {
            return DavOperationResult.noContent();
        }
        if (source.isCollection() && isNestedPath(normalizedSourcePath, normalizedDestinationPath)) {
            return DavOperationResult.conflict("Нельзя переместить папку в дочернюю папку.");
        }

        DavTarget target = resolveTarget(ownerId, normalizedDestinationPath, scopeRootFolderId);
        if (!target.valid()) {
            return DavOperationResult.conflict("Некорректный путь назначения.");
        }
        if (!target.parentExists()) {
            return DavOperationResult.conflict("Целевая папка не найдена.");
        }

        DavResource destination = target.existing();
        if (destination != null && !overwrite && !isSameResource(source, destination)) {
            return DavOperationResult.preconditionFailed("Ресурс назначения уже существует.");
        }
        if (destination != null && !isSameResource(source, destination)) {
            DavOperationResult deleteResult = delete(ownerId, normalizedDestinationPath, scopeRootFolderId);
            if (!deleteResult.success()) {
                return deleteResult;
            }
        }

        String error = source.isCollection()
                ? folderService.relocateByIdAndOwnerId(source.folder().getId(), ownerId, target.parentId(), target.name())
                : fileService.relocateByIdAndOwnerId(source.file().getId(), ownerId, target.parentId(), target.name());
        return error == null ? DavOperationResult.noContent() : DavOperationResult.conflict(error);
    }

    public DavOperationResult copy(Long ownerId, String sourcePath, String destinationPath, boolean overwrite) {
        return copy(ownerId, sourcePath, destinationPath, overwrite, null);
    }

    public DavOperationResult copy(Long ownerId, String sourcePath, String destinationPath, boolean overwrite, Long scopeRootFolderId) {
        DavResource source = resolve(ownerId, sourcePath, scopeRootFolderId);
        if (source == null) {
            return DavOperationResult.notFound("Исходный ресурс не найден.");
        }
        if (source.isRoot() || source.isScopedRoot()) {
            return DavOperationResult.methodNotAllowed("Корень диска копировать нельзя.");
        }

        String normalizedSourcePath = normalizePath(sourcePath);
        String normalizedDestinationPath = stripTrailingSlash(destinationPath);
        if (normalizedSourcePath.equals(normalizedDestinationPath)) {
            return DavOperationResult.noContent();
        }
        if (source.isCollection() && isNestedPath(normalizedSourcePath, normalizedDestinationPath)) {
            return DavOperationResult.conflict("Нельзя копировать папку в дочернюю папку.");
        }

        DavTarget target = resolveTarget(ownerId, normalizedDestinationPath, scopeRootFolderId);
        if (!target.valid()) {
            return DavOperationResult.conflict("Некорректный путь назначения.");
        }
        if (!target.parentExists()) {
            return DavOperationResult.conflict("Целевая папка не найдена.");
        }

        DavResource destination = target.existing();
        if (destination != null && !overwrite) {
            return DavOperationResult.preconditionFailed("Ресурс назначения уже существует.");
        }
        if (destination != null) {
            DavOperationResult deleteResult = delete(ownerId, normalizedDestinationPath, scopeRootFolderId);
            if (!deleteResult.success()) {
                return deleteResult;
            }
        }

        return source.isCollection()
                ? copyFolder(ownerId, source.folder(), target.parentId(), target.name())
                : copyFile(ownerId, source.file(), target.parentId(), target.name(), true);
    }

    private DavOperationResult copyFolder(Long ownerId, FolderDto sourceFolder, Long targetParentId, String targetName) {
        FolderDto createdFolder = new FolderDto();
        createdFolder.setOwnerId(ownerId);
        createdFolder.setParentId(targetParentId);
        createdFolder.setName(targetName);
        createdFolder.setPathKey(folderService.buildPathKeyForCreate(ownerId, targetParentId, targetName));
        createdFolder.setSortOrder(0);
        FolderDto savedFolder = folderService.save(createdFolder);
        if (savedFolder == null) {
            return DavOperationResult.serverError("Не удалось скопировать папку.");
        }

        for (FolderDto childFolder : folderService.findByOwnerIdAndParentId(ownerId, sourceFolder.getId())) {
            DavOperationResult folderResult = copyFolder(ownerId, childFolder, savedFolder.getId(), childFolder.getName());
            if (!folderResult.success()) {
                return folderResult;
            }
        }
        for (FileItemDto childFile : fileService.findByOwnerIdAndFolderId(ownerId, sourceFolder.getId())) {
            DavOperationResult fileResult = copyFile(ownerId, childFile, savedFolder.getId(), childFile.getOriginalFilename(), true);
            if (!fileResult.success()) {
                return fileResult;
            }
        }
        return DavOperationResult.created(savedFolder);
    }

    private DavOperationResult copyFile(Long ownerId,
                                        FileItemDto sourceFile,
                                        Long targetParentId,
                                        String targetName,
                                        boolean overwrite) {
        try (InputStream inputStream = fileService.openContentStream(sourceFile.getId(), ownerId)) {
            if (inputStream == null) {
                return DavOperationResult.notFound("Файл не найден.");
            }
            FileItemDto saved = fileService.uploadStream(ownerId, targetParentId, targetName, sourceFile.getMimeType(), inputStream, overwrite);
            return saved == null ? DavOperationResult.serverError("Не удалось скопировать файл.") : DavOperationResult.created(saved);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private DavTarget resolveTarget(Long ownerId, String path, Long scopeRootFolderId) {
        String normalizedPath = stripTrailingSlash(path);
        if ("/".equals(normalizedPath)) {
            return DavTarget.invalid();
        }

        int slashIndex = normalizedPath.lastIndexOf('/');
        String parentPath = slashIndex <= 0 ? "/" : normalizedPath.substring(0, slashIndex);
        String fileName = normalizedPath.substring(slashIndex + 1);
        String normalizedName = filenamePolicyService.normalizeFilename(fileName);
        if (normalizedName == null) {
            return DavTarget.invalid();
        }

        DavResource parent = resolve(ownerId, parentPath, scopeRootFolderId);
        if (parent == null) {
            return new DavTarget(false, false, null, null, normalizedName);
        }
        if (!parent.isRoot() && !parent.isCollection()) {
            return DavTarget.invalid();
        }

        Long parentId = parent.isRoot() && parent.folder() == null ? null : parent.folder().getId();
        DavResource existing = resolve(ownerId, normalizedPath, scopeRootFolderId);
        return new DavTarget(true, true, parentId, existing, normalizedName);
    }

    private FolderDto findFolderChild(Long ownerId, Long parentId, String name) {
        for (FolderDto folder : folderService.findByOwnerIdAndParentId(ownerId, parentId)) {
            if (equalsName(folder.getName(), name)) {
                return folder;
            }
        }
        return null;
    }

    private FileItemDto findFileChild(Long ownerId, Long parentId, String name) {
        for (FileItemDto file : fileService.findByOwnerIdAndFolderId(ownerId, parentId)) {
            if (equalsName(file.getOriginalFilename(), name)) {
                return file;
            }
        }
        return null;
    }

    private boolean equalsName(String left, String right) {
        return normalizedName(left).equals(normalizedName(right));
    }

    private String normalizedName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isSameResource(DavResource left, DavResource right) {
        if (left == null || right == null || left.type() != right.type()) {
            return false;
        }
        return left.id() != null && left.id().equals(right.id());
    }

    private boolean isNestedPath(String sourcePath, String destinationPath) {
        String sourcePrefix = sourcePath.endsWith("/") ? sourcePath : sourcePath + "/";
        return destinationPath.equals(sourcePath) || destinationPath.startsWith(sourcePrefix);
    }

    private List<String> splitPath(String path) {
        String normalizedPath = normalizePath(path);
        if ("/".equals(normalizedPath)) {
            return List.of();
        }

        String value = normalizedPath;
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("/"));
    }

    public String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String value = path.trim().replace('\\', '/');
        while (value.contains("//")) {
            value = value.replace("//", "/");
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            return value;
        }
        return value;
    }

    private String stripTrailingSlash(String path) {
        String normalized = normalizePath(path);
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record DavResource(Type type, FolderDto folder, FileItemDto file, boolean scopedRoot) {
        public static DavResource root() {
            return new DavResource(Type.ROOT, null, null, false);
        }

        public static DavResource scopedRoot(FolderDto folder) {
            return new DavResource(Type.ROOT, folder, null, true);
        }

        public static DavResource folder(FolderDto folder) {
            return new DavResource(Type.FOLDER, folder, null, false);
        }

        public static DavResource file(FileItemDto file) {
            return new DavResource(Type.FILE, null, file, false);
        }

        public boolean isRoot() {
            return type == Type.ROOT;
        }

        public boolean isCollection() {
            return type == Type.ROOT || type == Type.FOLDER;
        }

        public boolean isScopedRoot() {
            return scopedRoot;
        }

        public Long id() {
            if (folder != null) {
                return folder.getId();
            }
            return file == null ? null : file.getId();
        }

        public String name() {
            if (isRoot() && folder == null) {
                return "";
            }
            return folder != null ? folder.getName() : file.getOriginalFilename();
        }
    }

    public enum Type {
        ROOT,
        FOLDER,
        FILE
    }

    public record DavOperationResult(int status, Object resource, String message) {
        public static DavOperationResult created(Object resource) {
            return new DavOperationResult(201, resource, null);
        }

        public static DavOperationResult updated(Object resource) {
            return new DavOperationResult(204, resource, null);
        }

        public static DavOperationResult noContent() {
            return new DavOperationResult(204, null, null);
        }

        public static DavOperationResult notFound(String message) {
            return new DavOperationResult(404, null, message);
        }

        public static DavOperationResult conflict(String message) {
            return new DavOperationResult(409, null, message);
        }

        public static DavOperationResult preconditionFailed(String message) {
            return new DavOperationResult(412, null, message);
        }

        public static DavOperationResult methodNotAllowed(String message) {
            return new DavOperationResult(405, null, message);
        }

        public static DavOperationResult serverError(String message) {
            return new DavOperationResult(500, null, message);
        }

        public boolean success() {
            return status >= 200 && status < 300;
        }
    }

    private record DavTarget(boolean valid, boolean parentExists, Long parentId, DavResource existing, String name) {
        private static DavTarget invalid() {
            return new DavTarget(false, false, null, null, null);
        }
    }
}
