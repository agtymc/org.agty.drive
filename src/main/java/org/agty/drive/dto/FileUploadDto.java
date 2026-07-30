package org.agty.drive.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadDto {
    private Long folderId;
    private String description;
    private String expiresAt;
    private Boolean overwriteExisting;
    private MultipartFile file;
}
