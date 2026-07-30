package org.agty.drive.web.controllers.api;

import com.jayway.jsonpath.JsonPath;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.repository.ShareLinkRepository;
import org.agty.drive.security.service.DriveUserDetails;
import org.agty.drive.services.FileService;
import org.agty.drive.services.FolderService;
import org.agty.drive.services.ShareLinkService;
import org.agty.drive.services.UserService;
import org.agty.drive.support.IntegrationTestBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CabinetApiControllerTest extends IntegrationTestBootstrap {

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
    private ShareLinkRepository shareLinkRepository;

    @Test
    void shouldUploadDownloadAndPreviewFileViaApi() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), "api-upload-" + UUID.randomUUID());
        String filename = "api-file-" + UUID.randomUUID() + ".txt";
        String payload = "api payload " + UUID.randomUUID();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                payload.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/cabinet/files")
                        .file(multipartFile)
                        .param("folderId", String.valueOf(folder.getId()))
                        .param("description", "API upload")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.name").value(filename))
                .andReturn();

        Long fileId = Long.valueOf(JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.fileId").toString());
        FileItemDto file = fileService.findByIdAndOwnerId(fileId, user.getId());
        assertNotNull(file);

        mockMvc.perform(get("/api/cabinet/files/{id}/content", fileId)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/plain")))
                .andExpect(content().bytes(payload.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/cabinet/files/{id}/download", fileId)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(filename)))
                .andExpect(content().bytes(payload.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/cabinet/files/{id}/thumbnail", fileId)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUploadFileToRootViaApi() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        String filename = "api-root-file-" + UUID.randomUUID() + ".txt";
        String payload = "api root payload " + UUID.randomUUID();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                payload.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/cabinet/files")
                        .file(multipartFile)
                        .param("folderId", "")
                        .param("description", "API root upload")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.name").value(filename))
                .andReturn();

        Long fileId = Long.valueOf(JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.fileId").toString());
        FileItemDto file = fileService.findByIdAndOwnerId(fileId, user.getId());
        assertNotNull(file);
        assertNull(file.getFolderId());

        mockMvc.perform(get("/api/cabinet/files/{id}/content", fileId)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/plain")))
                .andExpect(content().bytes(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldCreateMoveRenameShareAndDeleteViaApi() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        String folderName = "api-folder-" + UUID.randomUUID();
        MvcResult createFolderResult = mockMvc.perform(post("/api/cabinet/folders")
                        .param("name", folderName)
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.name").value(folderName))
                .andReturn();

        Long folderId = Long.valueOf(JsonPath.read(createFolderResult.getResponse().getContentAsString(), "$.folderId").toString());
        FolderDto folder = folderService.findByIdAndOwnerId(folderId, user.getId());
        assertNotNull(folder);

        FolderDto targetFolder = createFolder(user.getId(), "api-target-" + UUID.randomUUID());
        FileItemDto file = createTextFile(user.getId(), folder.getId(), "api-move-" + UUID.randomUUID() + ".txt", "move payload");

        mockMvc.perform(post("/api/cabinet/items/rename")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .param("newName", "renamed-" + UUID.randomUUID() + ".txt")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.resourceType").value("FILE"));

        FileItemDto renamedFile = fileService.findByIdAndOwnerId(file.getId(), user.getId());
        assertNotNull(renamedFile);
        assertTrue(renamedFile.getOriginalFilename().startsWith("renamed-"));

        mockMvc.perform(post("/api/cabinet/items/move")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .param("targetFolderId", String.valueOf(targetFolder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        FileItemDto movedFile = fileService.findByIdAndOwnerId(file.getId(), user.getId());
        assertNotNull(movedFile);
        assertEquals(targetFolder.getId(), movedFile.getFolderId());

        MvcResult shareResult = mockMvc.perform(post("/api/cabinet/shares")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .param("allowPreview", "true")
                        .param("allowDownload", "true")
                        .param("expiresUnlimited", "true")
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("/s/")))
                .andReturn();

        String token = JsonPath.read(shareResult.getResponse().getContentAsString(), "$.token");
        assertNotNull(token);
        ShareLinkDto createdShareLink = shareLinkService.findByToken(token);
        assertNotNull(createdShareLink);
        assertTrue(createdShareLink.isWithoutExpiry());

        mockMvc.perform(get("/api/cabinet/shares/latest")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.token").value(token));

        mockMvc.perform(delete("/api/cabinet/shares")
                        .param("resourceType", "FILE")
                        .param("resourceId", String.valueOf(file.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        var disabledShare = shareLinkService.findByToken(token);
        assertNotNull(disabledShare);
        assertFalse(Boolean.TRUE.equals(disabledShare.getIsEnabled()));

        mockMvc.perform(delete("/api/cabinet/items/FILE/{resourceId}", file.getId())
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        mockMvc.perform(get("/api/cabinet/files/{id}/download", file.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnOnlyOwnedItemsInSharedLibraryApi() throws Exception {
        UserDto admin = userService.findByLogin("admin");
        assertNotNull(admin);

        FolderDto ownFolder = createFolder(admin.getId(), "api-own-shared-" + UUID.randomUUID());
        FileItemDto ownFile = createTextFile(admin.getId(), ownFolder.getId(), "api-own-shared-file-" + UUID.randomUUID() + ".txt", "own");
        ShareLinkDto ownShare = new ShareLinkDto();
        ownShare.setCreatedBy(admin.getId());
        ownShare.setToken("own" + UUID.randomUUID().toString().replace("-", ""));
        ownShare.setResourceType("FILE");
        ownShare.setResourceId(ownFile.getId());
        ownShare.setTitle(ownFile.getOriginalFilename());
        ownShare.setAllowDownload(true);
        ownShare.setAllowPreview(true);
        ownShare.setIsEnabled(true);
        ownShare.setDownloadCount(0L);
        assertNotNull(shareLinkRepository.save(ownShare));

        UserDto otherUser = createUser("api-shared-owner-" + UUID.randomUUID());
        FolderDto otherFolder = createFolder(otherUser.getId(), "api-foreign-shared-" + UUID.randomUUID());
        FileItemDto otherFile = createTextFile(otherUser.getId(), otherFolder.getId(), "api-foreign-shared-file-" + UUID.randomUUID() + ".txt", "foreign");
        ShareLinkDto foreignShare = new ShareLinkDto();
        foreignShare.setCreatedBy(admin.getId());
        foreignShare.setToken("foreign" + UUID.randomUUID().toString().replace("-", ""));
        foreignShare.setResourceType("FILE");
        foreignShare.setResourceId(otherFile.getId());
        foreignShare.setTitle(otherFile.getOriginalFilename());
        foreignShare.setAllowDownload(true);
        foreignShare.setAllowPreview(true);
        foreignShare.setIsEnabled(true);
        foreignShare.setDownloadCount(0L);
        assertNotNull(shareLinkRepository.save(foreignShare));

        mockMvc.perform(get("/api/cabinet/library")
                        .param("scope", "shared")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("shared"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ownFile.getOriginalFilename())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(otherFile.getOriginalFilename()))));
    }

    @Test
    void shouldFallbackBlockedUploadedMimeTypeToSafeResponseType() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), "api-mime-" + UUID.randomUUID());
        String filename = "mime-safe-" + UUID.randomUUID() + ".txt";
        String payload = "mime payload " + UUID.randomUUID();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/html",
                payload.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/cabinet/files")
                        .file(multipartFile)
                        .param("folderId", String.valueOf(folder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andReturn();

        Long fileId = Long.valueOf(JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.fileId").toString());
        FileItemDto file = fileService.findByIdAndOwnerId(fileId, user.getId());
        assertNotNull(file);
        assertEquals("text/plain", file.getMimeType());

        mockMvc.perform(get("/api/cabinet/files/{id}/content", fileId)
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/plain")))
                .andExpect(content().bytes(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldRejectUnsafeFilenameUploadViaApi() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), "api-unsafe-" + UUID.randomUUID());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "...",
                "text/plain",
                "bad".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/cabinet/files")
                        .file(multipartFile)
                        .param("folderId", String.valueOf(folder.getId()))
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("Некорректное имя файла."));
    }

    @Test
    void shouldExposeOpenAndMediaLibraryModesViaApi() throws Exception {
        UserDto user = userService.findByLogin("admin");
        assertNotNull(user);

        FolderDto folder = createFolder(user.getId(), "api-library-" + UUID.randomUUID());
        FileItemDto file = createTextFile(user.getId(), folder.getId(), "shared-open-" + UUID.randomUUID() + ".txt", "shared");

        var createDto = new org.agty.drive.dto.ShareLinkCreateDto();
        createDto.setResourceType("FILE");
        createDto.setResourceId(file.getId());
        createDto.setAllowPreview(true);
        createDto.setAllowDownload(true);
        createDto.setExpiresUnlimited(true);
        ShareLinkDto shareLink = shareLinkService.createShareLink(user.getId(), createDto);
        assertNotNull(shareLink);

        mockMvc.perform(get("/api/cabinet/library/open")
                        .param("type", "files")
                        .param("status", "without_expiry")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.scope").value("shared"))
                .andExpect(jsonPath("$.filters.status").value("without_expiry"))
                .andExpect(jsonPath("$.filters.type").value("files"))
                .andExpect(jsonPath("$.items[0].resourceName").value(file.getOriginalFilename()))
                .andExpect(jsonPath("$.items[0].shareLink.token").value(shareLink.getToken()));

        mockMvc.perform(get("/api/cabinet/library/media")
                        .param("scope", "videos")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.scope").value("videos"));

        mockMvc.perform(get("/api/cabinet/library/modes")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.modes[0].code").value("shared"))
                .andExpect(jsonPath("$.modes[3].code").value("collaborative"));
    }

    private FolderDto createFolder(Long ownerId, String name) {
        FolderDto folder = new FolderDto();
        folder.setOwnerId(ownerId);
        folder.setName(name);
        folder.setPathKey(folderService.buildPathKeyForCreate(ownerId, null, name));
        folder.setSortOrder(0);
        return folderService.save(folder);
    }

    private FileItemDto createTextFile(Long ownerId, Long folderId, String filename, String payload) {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                payload.getBytes(StandardCharsets.UTF_8)
        );
        org.agty.drive.dto.FileUploadDto dto = new org.agty.drive.dto.FileUploadDto();
        dto.setFolderId(folderId);
        dto.setDescription("api test file");
        dto.setFile(multipartFile);
        return fileService.upload(ownerId, dto);
    }

    private UserDto createUser(String login) {
        UserDto user = new UserDto();
        user.setLogin(login);
        user.setDisplayName(login);
        user.setPasswordHash("{noop}password");
        user.setRoleCode("ROLE_USER");
        user.setStatusCode("ACTIVE");
        user.setStorageQuotaBytes(100L * 1024L * 1024L);
        return userService.save(user);
    }
}
