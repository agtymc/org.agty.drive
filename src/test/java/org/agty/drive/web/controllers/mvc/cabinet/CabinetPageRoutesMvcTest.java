package org.agty.drive.web.controllers.mvc.cabinet;

import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.dto.OpenRegistrationDto;
import org.agty.drive.dto.ShareLinkCreateDto;
import org.agty.drive.dto.UserDto;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CabinetPageRoutesMvcTest extends IntegrationTestBootstrap {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private ShareLinkService shareLinkService;

    @Test
    void shouldRenderFilesPageRoute() throws Exception {
        UserDto user = userService.findByLogin("admin");
        FolderDto folder = createFolder(user.getId(), null, "files-page-" + UUID.randomUUID());
        createFile(user, folder, "files-page-item-" + UUID.randomUUID() + ".txt", "payload");

        mockMvc.perform(get("/cabinet")
                        .param("folderId", String.valueOf(folder.getId()))
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Управление")))
                .andExpect(content().string(containsString(folder.getName())));
    }

    @Test
    void shouldRenderPhotosPageRoute() throws Exception {
        UserDto user = userService.findByLogin("admin");
        FolderDto folder = createFolder(user.getId(), null, "photos-page-" + UUID.randomUUID());
        String imageName = "photos-page-item-" + UUID.randomUUID() + ".png";
        String noteName = "photos-page-note-" + UUID.randomUUID() + ".txt";
        createImageFile(user, folder, imageName);
        createFile(user, folder, noteName, "payload");

        var result = mockMvc.perform(get("/cabinet/photos")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Фото")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<FileItemDto> files = (List<FileItemDto>) result.getModelAndView().getModel().get("files");
        assertNotNull(files);
        assertTrue(files.stream().anyMatch(file -> imageName.equals(file.getOriginalFilename())));
        assertFalse(files.stream().anyMatch(file -> noteName.equals(file.getOriginalFilename())));
    }

    @Test
    void shouldRenderVideosPageRoute() throws Exception {
        UserDto user = userService.findByLogin("admin");
        FolderDto folder = createFolder(user.getId(), null, "videos-page-" + UUID.randomUUID());
        createVideoFile(user, folder, "videos-page-item-" + UUID.randomUUID() + ".mp4");
        createFile(user, folder, "videos-page-note-" + UUID.randomUUID() + ".txt", "payload");

        var result = mockMvc.perform(get("/cabinet/videos")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Видео")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<FileItemDto> files = (List<FileItemDto>) result.getModelAndView().getModel().get("files");
        assertNotNull(files);
        assertTrue(files.stream().anyMatch(file -> file.getOriginalFilename().endsWith(".mp4")));
        assertFalse(files.stream().anyMatch(file -> file.getOriginalFilename().startsWith("videos-page-note-")));
    }

    @Test
    void shouldRenderSharedPageRoute() throws Exception {
        UserDto user = userService.findByLogin("admin");
        FolderDto folder = createFolder(user.getId(), null, "shared-page-" + UUID.randomUUID());
        FileItemDto file = createFile(user, folder, "shared-page-item-" + UUID.randomUUID() + ".txt", "payload");

        ShareLinkCreateDto share = new ShareLinkCreateDto();
        share.setResourceType("FILE");
        share.setResourceId(file.getId());
        share.setAllowPreview(true);
        share.setAllowDownload(true);
        share.setExpiresUnlimited(true);
        var link = shareLinkService.createShareLink(user.getId(), share);

        mockMvc.perform(get("/cabinet/shared")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Открытый доступ")))
                .andExpect(content().string(containsString(file.getOriginalFilename())))
                .andExpect(content().string(containsString("/s/" + link.getToken())));
    }

    @Test
    void shouldRenderProfilePageRoute() throws Exception {
        UserDto user = userService.findByLogin("admin");

        mockMvc.perform(get("/cabinet/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Профиль")))
                .andExpect(content().string(containsString("К диску")))
                .andExpect(content().string(containsString("Смена пароля")))
                .andExpect(content().string(not(containsString("Управление"))));
    }

    @Test
    void shouldHideManagementButtonForRegularUser() throws Exception {
        UserDto user = createRegularUser();

        mockMvc.perform(get("/cabinet")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Управление"))));
    }

    @Test
    void shouldRejectControlPageForRegularUser() throws Exception {
        UserDto user = createRegularUser();

        mockMvc.perform(get("/control")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReuseCabinetShellForFilesAndProfilePages() throws Exception {
        UserDto user = userService.findByLogin("admin");

        mockMvc.perform(get("/cabinet")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"drive-sidebar\"")))
                .andExpect(content().string(containsString("class=\"sidebar-actions\"")))
                .andExpect(content().string(containsString("class=\"storage-card\"")));

        mockMvc.perform(get("/cabinet/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(new DriveUserDetails(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"drive-sidebar\"")))
                .andExpect(content().string(containsString("class=\"sidebar-actions\"")))
                .andExpect(content().string(containsString("class=\"storage-card\"")));
    }

    private FolderDto createFolder(Long ownerId, FolderDto parentFolder, String name) {
        FolderDto folderDto = new FolderDto();
        folderDto.setOwnerId(ownerId);
        folderDto.setParentId(parentFolder == null ? null : parentFolder.getId());
        folderDto.setName(name);
        folderDto.setPathKey(parentFolder == null ? "/" + name : parentFolder.getPathKey() + "/" + name);
        folderDto.setDescription("page route test folder");
        folderDto.setSortOrder(0);
        FolderDto saved = folderService.save(folderDto);
        assertNotNull(saved);
        return saved;
    }

    private FileItemDto createFile(UserDto user, FolderDto folder, String filename, String payload) {
        FileUploadDto fileUploadDto = new FileUploadDto();
        fileUploadDto.setFolderId(folder.getId());
        fileUploadDto.setDescription("page route test file");
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

    private FileItemDto createImageFile(UserDto user, FolderDto folder, String filename) {
        FileUploadDto fileUploadDto = new FileUploadDto();
        fileUploadDto.setFolderId(folder.getId());
        fileUploadDto.setDescription("page route image test file");
        fileUploadDto.setFile(new MockMultipartFile(
                "file",
                filename,
                "image/png",
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}
        ));
        FileItemDto saved = fileService.upload(user.getId(), fileUploadDto);
        assertNotNull(saved);
        return saved;
    }

    private FileItemDto createVideoFile(UserDto user, FolderDto folder, String filename) {
        FileUploadDto fileUploadDto = new FileUploadDto();
        fileUploadDto.setFolderId(folder.getId());
        fileUploadDto.setDescription("page route video test file");
        fileUploadDto.setFile(new MockMultipartFile(
                "file",
                filename,
                "video/mp4",
                "video-test-payload".getBytes(StandardCharsets.UTF_8)
        ));
        FileItemDto saved = fileService.upload(user.getId(), fileUploadDto);
        assertNotNull(saved);
        return saved;
    }

    private UserDto createRegularUser() {
        String login = "cabinet-user-" + UUID.randomUUID();
        OpenRegistrationDto registrationDto = new OpenRegistrationDto();
        registrationDto.setLogin(login);
        registrationDto.setDisplayName("Cabinet User");
        registrationDto.setEmail(login + "@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setConfirmPassword("password123");
        UserDto saved = userService.registerOpenUser(registrationDto);
        assertNotNull(saved);
        return saved;
    }
}
