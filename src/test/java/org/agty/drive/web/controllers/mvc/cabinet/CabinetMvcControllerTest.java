package org.agty.drive.web.controllers.mvc.cabinet;

import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.support.IntegrationTestBootstrap;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.ShareLinkService;
import org.agty.drive.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CabinetMvcControllerTest extends IntegrationTestBootstrap {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ShareLinkService shareLinkService;

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldUploadAndDownloadFileForAuthorizedUser() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = ensureFolder(user.getId());
        String filename = "mvc-upload-" + UUID.randomUUID() + ".txt";
        String payload = "AGTY/DRIVE upload test " + UUID.randomUUID();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                payload.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/cabinet/files/upload")
                        .file(multipartFile)
                        .param("folderId", String.valueOf(folder.getId()))
                        .param("description", "Тестовая загрузка")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + folder.getId()));

        List<FileItemDto> files = fileService.findByOwnerIdAndFolderId(user.getId(), folder.getId());
        FileItemDto uploadedFile = files.stream()
                .filter(item -> filename.equals(item.getOriginalFilename()))
                .findFirst()
                .orElse(null);

        assertNotNull(uploadedFile);
        assertNotNull(uploadedFile.getId());

        mockMvc.perform(get("/cabinet/files/{id}/download", uploadedFile.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(filename)))
                .andExpect(content().bytes(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldRenderShareDeleteConfirmationModal() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        mockMvc.perform(get("/cabinet")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-modal=\"share-delete-modal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Удалить ссылку?")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Доступ по ней сразу прекратится.")));
    }

    @Test
    void shouldDeleteShareLinkForAuthorizedUser() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = ensureFolder(user.getId());
        FileItemDto file = ensureFile(user, folder);

        ShareLinkCreateDto createDto = new ShareLinkCreateDto();
        createDto.setResourceType("FILE");
        createDto.setResourceId(file.getId());
        createDto.setAllowPreview(true);
        createDto.setAllowDownload(true);
        createDto.setExpiresUnlimited(true);

        var shareLink = shareLinkService.createShareLink(user.getId(), createDto);
        assertNotNull(shareLink);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/cabinet/shares/delete")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet"));

        assertTrue(!hasActiveShareLinkInDatabase(file.getId()));

        mockMvc.perform(get("/cabinet")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(shareLink.getToken()))));
    }

    @Test
    void shouldRenameFolderAndFileFromItemActions() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), null, "rename-folder-" + UUID.randomUUID());
        FileItemDto file = ensureFile(user, folder);

        mockMvc.perform(post("/cabinet/items/rename")
                        .param("resourceType", "FOLDER")
                        .param("resourceId", String.valueOf(folder.getId()))
                        .param("newName", "renamed-folder-" + UUID.randomUUID())
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet"));

        FolderDto renamedFolder = folderService.findByIdAndOwnerId(folder.getId(), user.getId());
        assertNotNull(renamedFolder);
        assertTrue(renamedFolder.getName().startsWith("renamed-folder-"));

        mockMvc.perform(post("/cabinet/items/rename")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .param("newName", "renamed-file-" + UUID.randomUUID() + ".txt")
                        .param("currentFolderId", String.valueOf(folder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + folder.getId()));

        FileItemDto renamedFile = fileService.findByIdAndOwnerId(file.getId(), user.getId());
        assertNotNull(renamedFile);
        assertTrue(renamedFile.getOriginalFilename().startsWith("renamed-file-"));
    }

    @Test
    void shouldMoveFolderAndFileFromItemActions() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto sourceFolder = createFolder(user.getId(), null, "source-" + UUID.randomUUID());
        FolderDto targetFolder = createFolder(user.getId(), null, "target-" + UUID.randomUUID());
        FolderDto childFolder = createFolder(user.getId(), sourceFolder, "child-" + UUID.randomUUID());
        FileItemDto file = ensureFile(user, sourceFolder);

        mockMvc.perform(post("/cabinet/items/move")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .param("targetFolderId", String.valueOf(targetFolder.getId()))
                        .param("currentFolderId", String.valueOf(sourceFolder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + sourceFolder.getId()));

        FileItemDto movedFile = fileService.findByIdAndOwnerId(file.getId(), user.getId());
        assertNotNull(movedFile);
        assertTrue(targetFolder.getId().equals(movedFile.getFolderId()));

        mockMvc.perform(post("/cabinet/items/move")
                        .param("resourceType", "FOLDER")
                        .param("resourceId", String.valueOf(childFolder.getId()))
                        .param("targetFolderId", String.valueOf(targetFolder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet"));

        FolderDto movedFolder = folderService.findByIdAndOwnerId(childFolder.getId(), user.getId());
        assertNotNull(movedFolder);
        assertTrue(targetFolder.getId().equals(movedFolder.getParentId()));
        assertTrue(movedFolder.getPathKey() != null && movedFolder.getPathKey().startsWith(targetFolder.getPathKey() + "/"));
    }

    @Test
    void shouldCreateFolderInsideCurrentFolder() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto parentFolder = createFolder(user.getId(), null, "parent-" + UUID.randomUUID());
        String childName = "child-" + UUID.randomUUID();

        mockMvc.perform(post("/cabinet/folders/add")
                        .param("parentId", String.valueOf(parentFolder.getId()))
                        .param("name", childName)
                        .param("description", "Вложенная папка")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + parentFolder.getId()));

        List<FolderDto> childFolders = folderService.findByOwnerIdAndParentId(user.getId(), parentFolder.getId());
        assertTrue(childFolders.stream().anyMatch(folder -> childName.equals(folder.getName())));
    }

    @Test
    void shouldDeleteFileFromItemActions() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), null, "delete-folder-" + UUID.randomUUID());
        FileItemDto file = createFile(user, folder, "delete-me-" + UUID.randomUUID() + ".txt", "delete payload");

        mockMvc.perform(post("/cabinet/items/delete")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .param("currentFolderId", String.valueOf(folder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + folder.getId()));

        assertTrue(hasDeletedFileInDatabase(file.getId()));
    }

    @Test
    void shouldPreserveCabinetStateForBulkOperations() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto currentFolder = createFolder(user.getId(), null, "bulk-current-" + UUID.randomUUID());
        FolderDto targetFolder = createFolder(user.getId(), null, "bulk-target-" + UUID.randomUUID());
        FileItemDto moveFile = createFile(user, currentFolder, "bulk-move-" + UUID.randomUUID() + ".txt", "move");

        mockMvc.perform(post("/cabinet/items/bulk/move")
                        .param("currentFolderId", String.valueOf(currentFolder.getId()))
                        .param("view", "grid")
                        .param("sort", "size_desc")
                        .param("q", "keep-search")
                        .param("scope", "tree")
                        .param("page", "2")
                        .param("size", "50")
                        .param("targetFolderId", String.valueOf(targetFolder.getId()))
                        .param("selectedFileIds", String.valueOf(moveFile.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + currentFolder.getId() + "&view=grid&sort=size_desc&q=keep-search&scope=tree&page=2&size=50"));

        FileItemDto movedFile = fileService.findByIdAndOwnerId(moveFile.getId(), user.getId());
        assertNotNull(movedFile);
        assertTrue(targetFolder.getId().equals(movedFile.getFolderId()));

        FileItemDto deleteFile = createFile(user, currentFolder, "bulk-delete-" + UUID.randomUUID() + ".txt", "delete");
        FolderDto deleteFolder = createFolder(user.getId(), currentFolder, "bulk-delete-folder-" + UUID.randomUUID());

        mockMvc.perform(post("/cabinet/items/bulk/delete")
                        .param("currentFolderId", String.valueOf(currentFolder.getId()))
                        .param("sort", "name_desc")
                        .param("q", "cleanup")
                        .param("scope", "all")
                        .param("page", "3")
                        .param("size", "100")
                        .param("selectedFileIds", String.valueOf(deleteFile.getId()))
                        .param("selectedFolderIds", String.valueOf(deleteFolder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + currentFolder.getId() + "&sort=name_desc&q=cleanup&scope=all&page=3&size=100"));

        assertTrue(hasDeletedFileInDatabase(deleteFile.getId()));
        assertTrue(hasDeletedFolderInDatabase(deleteFolder.getId()));
    }

    @Test
    void shouldReportPartialBulkMoveFailures() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto currentFolder = createFolder(user.getId(), null, "bulk-conflict-current-" + UUID.randomUUID());
        FolderDto targetFolder = createFolder(user.getId(), null, "bulk-conflict-target-" + UUID.randomUUID());
        FileItemDto conflictingSource = createFile(user, currentFolder, "same-name.txt", "source");
        FileItemDto successfulSource = createFile(user, currentFolder, "other-name.txt", "other");
        createFile(user, targetFolder, "same-name.txt", "target");

        mockMvc.perform(post("/cabinet/items/bulk/move")
                        .param("currentFolderId", String.valueOf(currentFolder.getId()))
                        .param("targetFolderId", String.valueOf(targetFolder.getId()))
                        .param("selectedFileIds", conflictingSource.getId() + "," + successfulSource.getId())
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("fileSuccess", "Перемещено файлов: 1, папок: 0."))
                .andExpect(flash().attribute("fileError", "Не удалось обработать объектов: 1."));

        FileItemDto unchangedFile = fileService.findByIdAndOwnerId(conflictingSource.getId(), user.getId());
        FileItemDto movedFile = fileService.findByIdAndOwnerId(successfulSource.getId(), user.getId());
        assertNotNull(unchangedFile);
        assertNotNull(movedFile);
        assertTrue(currentFolder.getId().equals(unchangedFile.getFolderId()));
        assertTrue(targetFolder.getId().equals(movedFile.getFolderId()));
    }

    @Test
    void shouldReportPartialBulkDeleteFailures() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto currentFolder = createFolder(user.getId(), null, "bulk-delete-errors-" + UUID.randomUUID());
        FileItemDto existingFile = createFile(user, currentFolder, "delete-existing-" + UUID.randomUUID() + ".txt", "payload");

        mockMvc.perform(post("/cabinet/items/bulk/delete")
                        .param("currentFolderId", String.valueOf(currentFolder.getId()))
                        .param("selectedFileIds", existingFile.getId() + ",999999999")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("fileSuccess", "Удалено файлов: 1, папок: 0."))
                .andExpect(flash().attribute("fileError", "Не удалось обработать объектов: 1."));

        assertTrue(hasDeletedFileInDatabase(existingFile.getId()));
    }

    @Test
    void shouldSortAndFilterCabinetEntries() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), null, "sort-folder-" + UUID.randomUUID());
        String alphaName = "alpha-sort-" + UUID.randomUUID() + ".txt";
        String zetaName = "zeta-sort-" + UUID.randomUUID() + ".txt";
        createFile(user, folder, alphaName, "alpha");
        createFile(user, folder, zetaName, "zeta");

        String sortedHtml = mockMvc.perform(get("/cabinet")
                        .param("folderId", String.valueOf(folder.getId()))
                        .param("sort", "name_desc")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(sortedHtml.indexOf(zetaName) < sortedHtml.indexOf(alphaName));

        mockMvc.perform(get("/cabinet")
                        .param("folderId", String.valueOf(folder.getId()))
                        .param("q", zetaName)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(zetaName)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(alphaName))));
    }

    @Test
    void shouldPaginateCabinetEntries() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), null, "page-folder-" + UUID.randomUUID());
        for (int i = 0; i < 22; i++) {
            createFile(user, folder, "page-item-%02d-%s.txt".formatted(i, UUID.randomUUID()), "payload-" + i);
        }

        String pageOneHtml = mockMvc.perform(get("/cabinet")
                        .param("folderId", String.valueOf(folder.getId()))
                        .param("size", "20")
                        .param("page", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String pageTwoHtml = mockMvc.perform(get("/cabinet")
                        .param("folderId", String.valueOf(folder.getId()))
                        .param("size", "20")
                        .param("page", "2")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(pageOneHtml.contains("page-item-00-"));
        assertTrue(pageOneHtml.contains("page-item-19-"));
        assertTrue(!pageOneHtml.contains("page-item-20-"));
        assertTrue(pageTwoHtml.contains("page-item-20-"));
        assertTrue(pageTwoHtml.contains("page-item-21-"));
    }

    @Test
    void shouldKeepFoldersAboveFilesDuringPagination() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        createFolder(user.getId(), null, "aaa-folder-" + UUID.randomUUID());
        createFolder(user.getId(), null, "bbb-folder-" + UUID.randomUUID());
        FolderDto fileFolder = createFolder(user.getId(), null, "file-page-holder-" + UUID.randomUUID());
        createFile(user, fileFolder, "aaa-file-" + UUID.randomUUID() + ".txt", "a");
        createFile(user, fileFolder, "bbb-file-" + UUID.randomUUID() + ".txt", "b");

        String html = mockMvc.perform(get("/cabinet")
                        .param("sort", "name_asc")
                        .param("size", "2")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(html.contains("aaa-folder-"));
        assertTrue(html.contains("bbb-folder-"));
        assertTrue(!html.contains("aaa-file-"));
    }

    @Test
    void shouldPreserveSearchStateInBreadcrumbsAndBackLink() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto parentFolder = createFolder(user.getId(), null, "nav-parent-" + UUID.randomUUID());
        FolderDto childFolder = createFolder(user.getId(), parentFolder, "nav-child-" + UUID.randomUUID());

        String html = mockMvc.perform(get("/cabinet")
                        .param("folderId", String.valueOf(childFolder.getId()))
                        .param("view", "grid")
                        .param("sort", "name_desc")
                        .param("q", "nav-check")
                        .param("scope", "tree")
                        .param("size", "50")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(html.contains("/cabinet?view=grid&amp;sort=name_desc&amp;q=nav-check&amp;scope=tree&amp;size=50"));
        assertTrue(html.contains("/cabinet?folderId=" + parentFolder.getId() + "&amp;view=grid&amp;sort=name_desc&amp;q=nav-check&amp;scope=tree&amp;size=50"));
    }

    @Test
    void shouldFindNestedFilesFromRootForTreeAndAllScopes() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto parentFolder = createFolder(user.getId(), null, "scope-parent-" + UUID.randomUUID());
        FolderDto childFolder = createFolder(user.getId(), parentFolder, "scope-child-" + UUID.randomUUID());
        String nestedFilename = "scope-target-" + UUID.randomUUID() + ".txt";
        createFile(user, childFolder, nestedFilename, "nested");

        mockMvc.perform(get("/cabinet")
                        .param("q", nestedFilename)
                        .param("scope", "tree")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(nestedFilename)));

        mockMvc.perform(get("/cabinet")
                        .param("q", nestedFilename)
                        .param("scope", "all")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(nestedFilename)));
    }

    @Test
    void shouldFindExistingIpFileFromRootWithAllScope() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);
        FolderDto folder = createFolder(user.getId(), null, "search-ip-" + UUID.randomUUID());
        createFile(user, folder, "IP1 - IP2.txt", "ip test payload");

        mockMvc.perform(get("/cabinet")
                        .param("view", "list")
                        .param("sort", "name_asc")
                        .param("size", "20")
                        .param("q", "IP")
                        .param("scope", "all")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("IP1 - IP2.txt")));
    }

    @Test
    void shouldDeleteFolderWithNestedContent() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto parentFolder = createFolder(user.getId(), null, "delete-parent-" + UUID.randomUUID());
        FolderDto childFolder = createFolder(user.getId(), parentFolder, "delete-child-" + UUID.randomUUID());
        FileItemDto nestedFile = createFile(user, childFolder, "nested-delete-" + UUID.randomUUID() + ".txt", "nested");

        mockMvc.perform(post("/cabinet/items/delete")
                        .param("resourceType", "FOLDER")
                        .param("resourceId", String.valueOf(parentFolder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet"));

        assertTrue(hasDeletedFolderInDatabase(parentFolder.getId()));
        assertTrue(hasDeletedFolderInDatabase(childFolder.getId()));
        assertTrue(hasDeletedFileInDatabase(nestedFile.getId()));
    }

    @Test
    void shouldCreatePublicShareAndKeepCabinetState() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), null, "share-folder-" + UUID.randomUUID());
        String filename = "share-check-" + UUID.randomUUID() + ".txt";
        FileItemDto file = createFile(user, folder, filename, "share");

        mockMvc.perform(post("/cabinet/shares")
                        .param("currentFolderId", String.valueOf(folder.getId()))
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .param("view", "grid")
                        .param("sort", "name_desc")
                        .param("q", "share-check")
                        .param("scope", "current")
                        .param("page", "2")
                        .param("size", "50")
                        .param("allowPreview", "true")
                        .param("allowDownload", "true")
                        .param("expiresUnlimited", "true")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cabinet?folderId=" + folder.getId() + "&view=grid&sort=name_desc&q=share-check&page=2&size=50"));

        var shareLink = shareLinkService.findLatestFileShareLink(file.getId());
        assertNotNull(shareLink);

        mockMvc.perform(get("/cabinet")
                        .param("folderId", String.valueOf(folder.getId()))
                        .param("view", "grid")
                        .param("sort", "name_desc")
                        .param("q", "share-check")
                        .param("size", "50")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/s/" + shareLink.getToken())));
    }

    @Test
    void shouldRejectMoveFolderIntoDescendant() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto rootFolder = createFolder(user.getId(), null, "root-" + UUID.randomUUID());
        FolderDto childFolder = createFolder(user.getId(), rootFolder, "desc-" + UUID.randomUUID());

        mockMvc.perform(post("/cabinet/items/move")
                        .param("resourceType", "FOLDER")
                        .param("resourceId", String.valueOf(rootFolder.getId()))
                        .param("targetFolderId", String.valueOf(childFolder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Нельзя переместить папку в дочернюю директорию.")));

        FolderDto unchangedFolder = folderService.findByIdAndOwnerId(rootFolder.getId(), user.getId());
        assertNotNull(unchangedFolder);
        assertTrue(unchangedFolder.getParentId() == null);
    }

    private FolderDto ensureFolder(Long ownerId) {
        List<FolderDto> folders = folderService.findRootFoldersByOwnerId(ownerId);
        if (!folders.isEmpty()) {
            return folders.getFirst();
        }

        FolderDto folderDto = new FolderDto();
        folderDto.setOwnerId(ownerId);
        folderDto.setName("Тестовая папка");
        folderDto.setPathKey("/test-folder");
        folderDto.setDescription("Создано интеграционным тестом");
        folderDto.setSortOrder(0);
        folderService.save(folderDto);

        List<FolderDto> savedFolders = folderService.findRootFoldersByOwnerId(ownerId);
        assertTrue(!savedFolders.isEmpty());
        return savedFolders.getFirst();
    }

    private FolderDto createFolder(Long ownerId, FolderDto parentFolder, String name) {
        FolderDto folderDto = new FolderDto();
        folderDto.setOwnerId(ownerId);
        folderDto.setParentId(parentFolder == null ? null : parentFolder.getId());
        folderDto.setName(name);
        folderDto.setPathKey(parentFolder == null ? "/" + name : parentFolder.getPathKey() + "/" + name);
        folderDto.setDescription("Тестовая папка");
        folderDto.setSortOrder(0);
        FolderDto saved = folderService.save(folderDto);
        assertNotNull(saved);
        return saved;
    }

    private FileItemDto ensureFile(UserDto user, FolderDto folder) {
        String filename = "share-delete-" + UUID.randomUUID() + ".txt";
        String payload = "share delete payload " + UUID.randomUUID();
        return createFile(user, folder, filename, payload);
    }

    private FileItemDto createFile(UserDto user, FolderDto folder, String filename, String payload) {
        FileUploadDto fileUploadDto = new FileUploadDto();
        fileUploadDto.setFolderId(folder.getId());
        fileUploadDto.setDescription("Файл для теста удаления ссылки");
        fileUploadDto.setFile(new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                payload.getBytes(StandardCharsets.UTF_8)
        ));
        FileItemDto saved = fileService.upload(user.getId(), fileUploadDto);
        assertNotNull(saved);
        return saved;
    }

    private boolean hasActiveShareLinkInDatabase(Long resourceId) throws Exception {
        String query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM public.agdrv_share_links
                    WHERE resource_type = 'FILE'
                      AND resource_id = ?
                      AND is_enabled = TRUE
                )
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, resourceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private boolean hasDeletedFileInDatabase(Long fileId) throws Exception {
        String query = """
                SELECT deleted_at IS NOT NULL
                FROM public.agdrv_files
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, fileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private boolean hasDeletedFolderInDatabase(Long folderId) throws Exception {
        String query = """
                SELECT deleted_at IS NOT NULL
                FROM public.agdrv_folders
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, folderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }
}
