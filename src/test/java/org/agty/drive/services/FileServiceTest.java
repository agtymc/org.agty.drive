package org.agty.drive.services;

import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.repository.FileRepository;
import org.agty.drive.repository.ShareLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    @Test
    void shouldRejectUploadWithUnsafeFilename() {
        FileRepository fileRepository = mock(FileRepository.class);
        FolderService folderService = mock(FolderService.class);
        FileContentStorageService fileContentStorageService = mock(FileContentStorageService.class);
        ImageThumbnailService imageThumbnailService = mock(ImageThumbnailService.class);
        UserService userService = mock(UserService.class);
        ShareLinkRepository shareLinkRepository = mock(ShareLinkRepository.class);

        when(userService.findById(1L)).thenReturn(new UserDto());

        FileService fileService = new FileService(
                fileRepository,
                folderService,
                fileContentStorageService,
                imageThumbnailService,
                userService,
                new FilenamePolicyService(),
                new MimeTypePolicyService(),
                new ExpirationPolicyService(),
                shareLinkRepository
        );

        FileUploadDto dto = new FileUploadDto();
        dto.setFolderId(10L);
        dto.setFile(new MockMultipartFile("file", "...", "text/plain", "bad".getBytes()));

        assertEquals("Некорректное имя файла.", fileService.validateUpload(1L, dto));
    }

    @Test
    void shouldAllowUploadToRootWithoutFolders() {
        FileRepository fileRepository = mock(FileRepository.class);
        FolderService folderService = mock(FolderService.class);
        FileContentStorageService fileContentStorageService = mock(FileContentStorageService.class);
        ImageThumbnailService imageThumbnailService = mock(ImageThumbnailService.class);
        UserService userService = mock(UserService.class);
        ShareLinkRepository shareLinkRepository = mock(ShareLinkRepository.class);

        UserDto userDto = new UserDto();
        userDto.setStorageQuotaBytes(1024L * 1024L);
        when(userService.findById(1L)).thenReturn(userDto);
        when(fileRepository.sumSizeByOwnerId(1L)).thenReturn(0L);
        when(fileRepository.existsByOwnerIdAndFolderIdAndOriginalFilename(1L, null, "root.txt", null)).thenReturn(false);

        FileService fileService = new FileService(
                fileRepository,
                folderService,
                fileContentStorageService,
                imageThumbnailService,
                userService,
                new FilenamePolicyService(),
                new MimeTypePolicyService(),
                new ExpirationPolicyService(),
                shareLinkRepository
        );

        FileUploadDto dto = new FileUploadDto();
        dto.setFolderId(null);
        dto.setFile(new MockMultipartFile("file", "root.txt", "text/plain", "ok".getBytes()));

        assertNull(fileService.validateUpload(1L, dto));
        verify(folderService, never()).findByIdAndOwnerId(null, 1L);
    }
}
