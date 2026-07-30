package org.agty.drive.dto;

import lombok.Data;

@Data
public class SharedLibraryItemDto {
    private ShareLinkDto shareLink;
    private String resourceName;
    private String resourceTypeTitle;
    private String resourceMimeType;
    private Long resourceSizeBytes;
    private boolean previewAvailable;
    private String previewType;

    public SharedLibraryItemDto(ShareLinkDto shareLink, String resourceName, String resourceTypeTitle) {
        this.shareLink = shareLink;
        this.resourceName = resourceName;
        this.resourceTypeTitle = resourceTypeTitle;
    }

    public boolean isFolder() {
        return shareLink != null && "FOLDER".equalsIgnoreCase(shareLink.getResourceType());
    }

    public boolean isFile() {
        return !isFolder();
    }

    public String getStatusCode() {
        return shareLink == null ? "active" : shareLink.getExpiryStatusCode();
    }

    public String getStatusTitle() {
        return shareLink == null ? "Активна" : shareLink.getExpiryStatusTitle();
    }
}
