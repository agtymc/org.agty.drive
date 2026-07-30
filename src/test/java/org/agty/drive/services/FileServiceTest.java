package org.agty.drive.services;

import org.agty.drive.dto.FileUploadDto;
import org.agty.drive.dto.UserDto;
import org.agty.drive.repository.FileRepository;
import org.agty.drive.repository.ShareLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
}
