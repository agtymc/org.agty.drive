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

package org.agty.drive.web.controllers.mvc.share;

import jakarta.servlet.http.HttpSession;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.services.FileContentStorageService;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderArchiveService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.MimeTypePolicyService;
import org.agty.drive.services.ShareLinkService;
import org.agty.drive.web.MediaResponseSupport;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/s")
public class ShareMvcController {

    private final ShareLinkService shareLinkService;
    private final FileService fileService;
    private final FileContentStorageService fileContentStorageService;
    private final FolderService folderService;
    private final FolderArchiveService folderArchiveService;
    private final MimeTypePolicyService mimeTypePolicyService;

    public ShareMvcController(ShareLinkService shareLinkService,
                              FileService fileService,
                              FileContentStorageService fileContentStorageService,
                              FolderService folderService,
                              FolderArchiveService folderArchiveService,
                              MimeTypePolicyService mimeTypePolicyService) {
        this.shareLinkService = shareLinkService;
        this.fileService = fileService;
        this.fileContentStorageService = fileContentStorageService;
        this.folderService = folderService;
        this.folderArchiveService = folderArchiveService;
        this.mimeTypePolicyService = mimeTypePolicyService;
    }

    @GetMapping("{token}")
    public String view(@PathVariable("token") String token,
                       @RequestParam(name = "folderId", required = false) Long folderId,
                       @RequestParam(name = "q", required = false) String searchQuery,
                       @RequestParam(name = "page", required = false) Integer page,
                       @RequestParam(name = "size", required = false) Integer pageSize,
                       HttpSession session,
                       Model model) {
        ShareLinkDto shareLink = shareLinkService.findByToken(token);
        if (!shareLinkService.isAccessible(shareLink)) {
            model.addAttribute("pageTitle", "Публичная ссылка");
            model.addAttribute("shareError", "Ссылка недоступна или срок ее действия истек.");
            return "share/view";
        }

        if ("FOLDER".equalsIgnoreCase(shareLink.getResourceType())) {
            return buildFolderView(model, shareLink, session, folderId, searchQuery, page, pageSize, null);
        }
        return buildFileView(model, shareLink, session, null);
    }

    @PostMapping("{token}/unlock")
    public String unlock(@PathVariable("token") String token,
                         @RequestParam("password") String password,
                         @RequestParam(name = "folderId", required = false) Long folderId,
                         @RequestParam(name = "q", required = false) String searchQuery,
                         @RequestParam(name = "page", required = false) Integer page,
                         @RequestParam(name = "size", required = false) Integer pageSize,
                         HttpSession session,
                         Model model) {
        ShareLinkDto shareLink = shareLinkService.findByToken(token);
        if (!shareLinkService.isAccessible(shareLink)) {
            model.addAttribute("pageTitle", "Публичная ссылка");
            model.addAttribute("shareError", "Ссылка недоступна или срок ее действия истек.");
            return "share/view";
        }

        if (!shareLinkService.verifyPassword(shareLink, password)) {
            if ("FOLDER".equalsIgnoreCase(shareLink.getResourceType())) {
                return buildFolderView(model, shareLink, session, folderId, searchQuery, page, pageSize, "Неверный пароль.");
            }
            return buildFileView(model, shareLink, session, "Неверный пароль.");
        }

        session.setAttribute(accessKey(shareLink.getId()), Boolean.TRUE);
        return redirectShareFolder(token, folderId, searchQuery, page, pageSize);
    }

    @GetMapping("{token}/download")
    public ResponseEntity<?> download(@PathVariable("token") String token,
                                      @RequestParam(name = "folderId", required = false) Long folderId,
                                      HttpSession session) {
        ShareLinkDto shareLink = shareLinkService.findByToken(token);
        if (!shareLinkService.isAccessible(shareLink)
                || !Boolean.TRUE.equals(shareLink.getAllowDownload())
                || !isUnlocked(session, shareLink)) {
            return ResponseEntity.notFound().build();
        }

        if ("FOLDER".equalsIgnoreCase(shareLink.getResourceType())) {
            FolderDto rootFolder = resolveFolder(shareLink);
            FolderDto currentFolder = resolveFolderInTree(rootFolder, folderId);
            if (currentFolder == null) {
                return ResponseEntity.notFound().build();
            }

            var archivePath = folderArchiveService.buildFolderArchiveTempFile(currentFolder);
            if (archivePath == null) {
                return ResponseEntity.notFound().build();
            }

            shareLinkService.registerDownload(shareLink);
            return MediaResponseSupport.buildEphemeralPathResponse(
                    archivePath,
                    MediaType.APPLICATION_OCTET_STREAM,
                    currentFolder.getName() + ".zip",
                    false,
                    null
            );
        }

        FileItemDto file = resolveFile(shareLink);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        var path = fileContentStorageService.resolveExistingPath(file.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        shareLinkService.registerDownload(shareLink);
        return MediaResponseSupport.buildPathResponse(
                path,
                resolveMediaType(file),
                file.getOriginalFilename(),
                false,
                null
        );
    }

    @GetMapping("{token}/content")
    public ResponseEntity<?> content(@PathVariable("token") String token,
                                     @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                                     HttpSession session) {
        ShareLinkDto shareLink = shareLinkService.findByToken(token);
        if (!shareLinkService.isAccessible(shareLink)
                || !Boolean.TRUE.equals(shareLink.getAllowPreview())
                || !isUnlocked(session, shareLink)) {
            return ResponseEntity.notFound().build();
        }

        FileItemDto file = resolveFile(shareLink);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        var path = fileContentStorageService.resolveExistingPath(file.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        return MediaResponseSupport.buildPathResponse(
                path,
                resolveMediaType(file),
                file.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @GetMapping("{token}/files/{fileId}/download")
    public ResponseEntity<?> downloadSharedFolderFile(@PathVariable("token") String token,
                                                      @PathVariable("fileId") Long fileId,
                                                      HttpSession session) {
        ShareLinkDto shareLink = shareLinkService.findByToken(token);
        if (!shareLinkService.isAccessible(shareLink)
                || !Boolean.TRUE.equals(shareLink.getAllowDownload())
                || !isUnlocked(session, shareLink)) {
            return ResponseEntity.notFound().build();
        }

        FolderDto rootFolder = resolveFolder(shareLink);
        FileItemDto file = resolveFileInSharedFolder(rootFolder, fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        var path = fileContentStorageService.resolveExistingPath(file.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        shareLinkService.registerDownload(shareLink);
        return MediaResponseSupport.buildPathResponse(
                path,
                resolveMediaType(file),
                file.getOriginalFilename(),
                false,
                null
        );
    }

    @GetMapping("{token}/files/{fileId}/content")
    public ResponseEntity<?> contentSharedFolderFile(@PathVariable("token") String token,
                                                     @PathVariable("fileId") Long fileId,
                                                     @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                                                     HttpSession session) {
        ShareLinkDto shareLink = shareLinkService.findByToken(token);
        if (!shareLinkService.isAccessible(shareLink)
                || !Boolean.TRUE.equals(shareLink.getAllowPreview())
                || !isUnlocked(session, shareLink)) {
            return ResponseEntity.notFound().build();
        }

        FolderDto rootFolder = resolveFolder(shareLink);
        FileItemDto file = resolveFileInSharedFolder(rootFolder, fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        var path = fileContentStorageService.resolveExistingPath(file.getStorageName());
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        return MediaResponseSupport.buildPathResponse(
                path,
                resolveMediaType(file),
                file.getOriginalFilename(),
                true,
                rangeHeader
        );
    }

    @GetMapping("{token}/files/{fileId}/thumbnail")
    public ResponseEntity<byte[]> thumbnailSharedFolderFile(@PathVariable("token") String token,
                                                            @PathVariable("fileId") Long fileId,
                                                            HttpSession session) {
        ShareLinkDto shareLink = shareLinkService.findByToken(token);
        if (!shareLinkService.isAccessible(shareLink) || !isUnlocked(session, shareLink)) {
            return ResponseEntity.notFound().build();
        }

        FolderDto rootFolder = resolveFolder(shareLink);
        FileItemDto file = resolveFileInSharedFolder(rootFolder, fileId);
        if (file == null || !file.isImagePreview()) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = fileService.findThumbnailBytesByFileId(file.getId());
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(content.length)
                .body(content);
    }

    private String buildFileView(Model model, ShareLinkDto shareLink, HttpSession session, String shareError) {
        FileItemDto file = resolveFile(shareLink);
        if (file == null) {
            model.addAttribute("pageTitle", "Файл не найден");
            model.addAttribute("shareTitle", "Файл не найден");
            model.addAttribute("shareError", "Файл по ссылке не найден.");
            return "share/view";
        }

        model.addAttribute("pageTitle", "Публичная ссылка");
        model.addAttribute("shareLink", shareLink);
        model.addAttribute("sharedFile", file);
        model.addAttribute("shareUnlocked", isUnlocked(session, shareLink));
        if (shareError != null) {
            model.addAttribute("shareError", shareError);
        }
        return "share/view";
    }

    private String buildFolderView(Model model,
                                   ShareLinkDto shareLink,
                                   HttpSession session,
                                   Long folderId,
                                   String searchQuery,
                                   Integer page,
                                   Integer pageSize,
                                   String shareError) {
        FolderDto rootFolder = resolveFolder(shareLink);
        if (rootFolder == null) {
            model.addAttribute("pageTitle", "Папка не найдена");
            model.addAttribute("shareTitle", "Папка не найдена");
            model.addAttribute("shareError", "Папка по ссылке не найдена.");
            return "share/view";
        }

        FolderDto currentFolder = resolveFolderInTree(rootFolder, folderId);
        if (currentFolder == null) {
            model.addAttribute("pageTitle", "Папка не найдена");
            model.addAttribute("shareTitle", "Папка не найдена");
            model.addAttribute("shareError", "Папка по ссылке не найдена.");
            return "share/view";
        }

        boolean unlocked = isUnlocked(session, shareLink);
        model.addAttribute("pageTitle", "Публичная ссылка");
        model.addAttribute("shareTitle", currentFolder.getName());
        model.addAttribute("shareLink", shareLink);
        model.addAttribute("sharedFolderRoot", rootFolder);
        model.addAttribute("sharedFolder", currentFolder);
        model.addAttribute("shareUnlocked", unlocked);
        if (unlocked && Boolean.TRUE.equals(shareLink.getAllowPreview())) {
            List<FolderDto> sharedFolders = folderService.findByOwnerIdAndParentId(rootFolder.getOwnerId(), currentFolder.getId());
            List<FileItemDto> sharedFiles = fileService.findByOwnerIdAndFolderId(rootFolder.getOwnerId(), currentFolder.getId());
            String normalizedQuery = normalizeSearchQuery(searchQuery);
            sharedFolders = filterFolders(sharedFolders, normalizedQuery);
            sharedFiles = filterFiles(sharedFiles, normalizedQuery);
            SharedFolderPage folderPage = paginateSharedEntries(sharedFolders, sharedFiles, page, pageSize);
            model.addAttribute("sharedFolders", folderPage.folders());
            model.addAttribute("sharedFiles", folderPage.files());
            model.addAttribute("sharedFolderBreadcrumbs", buildSharedFolderBreadcrumbs(rootFolder, currentFolder));
            model.addAttribute("sharedSearchQuery", normalizedQuery);
            model.addAttribute("sharedCurrentPage", folderPage.currentPage());
            model.addAttribute("sharedTotalPages", folderPage.totalPages());
            model.addAttribute("sharedPageSize", folderPage.pageSize());
            model.addAttribute("sharedTotalItems", folderPage.totalItems());
            model.addAttribute("sharedSearchActive", !normalizedQuery.isBlank());
            model.addAttribute("pageSizeOptions", List.of(20, 50, 100));
        } else {
            model.addAttribute("sharedFolders", new ArrayList<>());
            model.addAttribute("sharedFiles", new ArrayList<>());
            model.addAttribute("sharedFolderBreadcrumbs", new ArrayList<>());
            model.addAttribute("sharedSearchQuery", "");
            model.addAttribute("sharedCurrentPage", 1);
            model.addAttribute("sharedTotalPages", 1);
            model.addAttribute("sharedPageSize", 20);
            model.addAttribute("sharedTotalItems", 0);
            model.addAttribute("sharedSearchActive", false);
            model.addAttribute("pageSizeOptions", List.of(20, 50, 100));
        }
        if (shareError != null) {
            model.addAttribute("shareError", shareError);
        }
        return "share/view";
    }

    private boolean isUnlocked(HttpSession session, ShareLinkDto shareLink) {
        if (shareLink == null) {
            return false;
        }
        if (shareLink.getPasswordHash() == null || shareLink.getPasswordHash().isBlank()) {
            return true;
        }
        Object unlocked = session.getAttribute(accessKey(shareLink.getId()));
        return Boolean.TRUE.equals(unlocked);
    }

    private String accessKey(Long shareId) {
        return "share_access_" + shareId;
    }

    private FileItemDto resolveFile(ShareLinkDto shareLink) {
        if (shareLink == null || !"FILE".equalsIgnoreCase(shareLink.getResourceType())) {
            return null;
        }
        return fileService.findById(shareLink.getResourceId());
    }

    private FolderDto resolveFolder(ShareLinkDto shareLink) {
        if (shareLink == null || !"FOLDER".equalsIgnoreCase(shareLink.getResourceType())) {
            return null;
        }
        return folderService.findById(shareLink.getResourceId());
    }

    private FolderDto resolveFolderInTree(FolderDto rootFolder, Long folderId) {
        if (rootFolder == null) {
            return null;
        }
        if (folderId == null || folderId.equals(rootFolder.getId())) {
            return rootFolder;
        }
        FolderDto candidate = folderService.findById(folderId);
        if (candidate == null || !rootFolder.getOwnerId().equals(candidate.getOwnerId())) {
            return null;
        }
        return isFolderInsideTree(rootFolder, candidate) ? candidate : null;
    }

    private boolean isFolderInsideTree(FolderDto rootFolder, FolderDto candidateFolder) {
        FolderDto pointer = candidateFolder;
        while (pointer != null) {
            if (pointer.getId().equals(rootFolder.getId())) {
                return true;
            }
            Long parentId = pointer.getParentId();
            pointer = parentId == null ? null : folderService.findById(parentId);
        }
        return false;
    }

    private FileItemDto resolveFileInSharedFolder(FolderDto rootFolder, Long fileId) {
        if (rootFolder == null || fileId == null) {
            return null;
        }
        FileItemDto file = fileService.findById(fileId);
        if (file == null || !rootFolder.getOwnerId().equals(file.getOwnerId())) {
            return null;
        }
        if (file.getFolderId() == null) {
            return null;
        }
        FolderDto folder = folderService.findById(file.getFolderId());
        return folder != null && isFolderInsideTree(rootFolder, folder) ? file : null;
    }

    private List<FolderDto> buildSharedFolderBreadcrumbs(FolderDto rootFolder, FolderDto currentFolder) {
        LinkedList<FolderDto> breadcrumbs = new LinkedList<>();
        FolderDto pointer = currentFolder;
        while (pointer != null) {
            breadcrumbs.addFirst(pointer);
            if (pointer.getId().equals(rootFolder.getId()) || pointer.getParentId() == null) {
                break;
            }
            pointer = folderService.findById(pointer.getParentId());
        }
        return breadcrumbs;
    }

    private MediaType resolveMediaType(FileItemDto file) {
        return mimeTypePolicyService.resolveResponseMediaType(file == null ? null : file.getMimeType());
    }

    private String normalizeSearchQuery(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.isBlank() ? "" : normalized;
    }

    private int normalizePage(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private int normalizePageSize(Integer value) {
        if (value == null) {
            return 20;
        }
        return switch (value) {
            case 50, 100 -> value;
            default -> 20;
        };
    }

    private String redirectShareFolder(String token, Long folderId, String searchQuery, Integer page, Integer pageSize) {
        StringBuilder builder = new StringBuilder("redirect:/s/").append(token);
        List<String> params = new ArrayList<>();
        if (folderId != null) {
            params.add("folderId=" + folderId);
        }
        String normalizedQuery = normalizeSearchQuery(searchQuery);
        if (!normalizedQuery.isBlank()) {
            params.add("q=" + normalizedQuery);
        }
        int normalizedPage = normalizePage(page);
        if (normalizedPage > 1) {
            params.add("page=" + normalizedPage);
        }
        int normalizedPageSize = normalizePageSize(pageSize);
        if (normalizedPageSize != 20) {
            params.add("size=" + normalizedPageSize);
        }
        if (!params.isEmpty()) {
            builder.append("?").append(String.join("&", params));
        }
        return builder.toString();
    }

    private List<FolderDto> filterFolders(List<FolderDto> folders, String query) {
        if (query == null || query.isBlank()) {
            return folders;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return folders.stream()
                .filter(folder -> safeLower(folder.getName()).contains(needle))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private List<FileItemDto> filterFiles(List<FileItemDto> files, String query) {
        if (query == null || query.isBlank()) {
            return files;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return files.stream()
                .filter(file -> safeLower(file.getOriginalFilename()).contains(needle))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private SharedFolderPage paginateSharedEntries(List<FolderDto> folders, List<FileItemDto> files, Integer page, Integer pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        int totalItems = folders.size() + files.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) normalizedPageSize));
        int currentPage = Math.min(normalizePage(page), totalPages);
        int fromIndex = Math.max(0, (currentPage - 1) * normalizedPageSize);
        int toIndex = Math.min(totalItems, fromIndex + normalizedPageSize);

        List<Object> entries = new ArrayList<>(totalItems);
        entries.addAll(folders);
        entries.addAll(files);

        List<FolderDto> pageFolders = new ArrayList<>();
        List<FileItemDto> pageFiles = new ArrayList<>();
        for (Object entry : entries.subList(fromIndex, toIndex)) {
            if (entry instanceof FolderDto folder) {
                pageFolders.add(folder);
            } else if (entry instanceof FileItemDto file) {
                pageFiles.add(file);
            }
        }
        return new SharedFolderPage(pageFolders, pageFiles, currentPage, totalPages, normalizedPageSize, totalItems);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record SharedFolderPage(List<FolderDto> folders,
                                    List<FileItemDto> files,
                                    int currentPage,
                                    int totalPages,
                                    int pageSize,
                                    int totalItems) {
    }
}
