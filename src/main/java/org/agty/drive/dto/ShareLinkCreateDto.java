package org.agty.drive.dto;

import lombok.Data;

@Data
public class ShareLinkCreateDto {
    private String resourceType;
    private Long resourceId;
    private String title;
    private String password;
    private Integer expiresInHours;
    private Boolean expiresUnlimited;
    private Boolean allowDownload;
    private Boolean allowPreview;
}
