package org.agty.drive.dto;

import lombok.Data;
import org.agty.drive.config.AppTime;
import org.agty.utils.AgtyUtils;

@Data
public class FileItemDto {
    private Long id;
    private Long ownerId;
    private Long folderId;
    private String folderName;
    private String originalFilename;
    private String storageName;
    private String mimeType;
    private String extension;
    private Long sizeBytes;
    private String checksumSha256;
    private String description;
    private String previewStatus;
    private Boolean isImage;
    private Boolean isVideo;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public String getSizeTitle() {
        if (sizeBytes == null || sizeBytes <= 0) {
            return "0 Б";
        }
        return AgtyUtils.filesizeToTitle(sizeBytes, "ru");
    }

    public String getCreatedAtTitle() {
        return convertDateTime(createdAt);
    }

    public String getUpdatedAtTitle() {
        return convertDateTime(updatedAt);
    }

    public String getIconType() {
        if (isImagePreview()) {
            return "image";
        }
        if (isVideoPreview()) {
            return "video";
        }
        String ext = extension == null ? "" : extension.toLowerCase();
        if ("pdf".equals(ext)) return "pdf";
        if ("doc".equals(ext) || "docx".equals(ext) || "rtf".equals(ext) || "odt".equals(ext)) return "doc";
        if ("xls".equals(ext) || "xlsx".equals(ext) || "csv".equals(ext) || "ods".equals(ext)) return "sheet";
        if ("zip".equals(ext) || "rar".equals(ext) || "7z".equals(ext) || "tar".equals(ext) || "gz".equals(ext)) return "archive";
        if ("mp3".equals(ext) || "wav".equals(ext) || "flac".equals(ext) || "ogg".equals(ext)) return "audio";
        if ("txt".equals(ext) || "md".equals(ext) || "json".equals(ext) || "xml".equals(ext) || "yml".equals(ext)) return "text";
        return "file";
    }

    public String getExtensionTitle() {
        if (extension == null || extension.isBlank()) {
            return "FILE";
        }
        return extension.length() > 4 ? extension.substring(0, 4).toUpperCase() : extension.toUpperCase();
    }

    public boolean isImagePreview() {
        if (Boolean.TRUE.equals(isImage)) {
            return true;
        }
        String mime = mimeType == null ? "" : mimeType.toLowerCase();
        if (mime.startsWith("image/")) {
            return true;
        }
        String ext = extension == null ? "" : extension.toLowerCase();
        return "jpg".equals(ext)
                || "jpeg".equals(ext)
                || "png".equals(ext)
                || "gif".equals(ext)
                || "webp".equals(ext)
                || "bmp".equals(ext)
                || "svg".equals(ext)
                || "ico".equals(ext)
                || "tif".equals(ext)
                || "tiff".equals(ext);
    }

    public boolean isThumbnailPreviewReady() {
        return isImagePreview() && "READY".equalsIgnoreCase(previewStatus);
    }

    public boolean isVideoPreview() {
        if (Boolean.TRUE.equals(isVideo)) {
            return true;
        }
        String mime = mimeType == null ? "" : mimeType.toLowerCase();
        if (mime.startsWith("video/")) {
            return true;
        }
        String ext = extension == null ? "" : extension.toLowerCase();
        return "mp4".equals(ext)
                || "webm".equals(ext)
                || "mov".equals(ext)
                || "avi".equals(ext)
                || "mkv".equals(ext)
                || "mpeg".equals(ext)
                || "mpg".equals(ext)
                || "m4v".equals(ext);
    }

    public boolean isAudioPreview() {
        String mime = mimeType == null ? "" : mimeType.toLowerCase();
        if (mime.startsWith("audio/")) {
            return true;
        }
        String ext = extension == null ? "" : extension.toLowerCase();
        return "mp3".equals(ext)
                || "wav".equals(ext)
                || "flac".equals(ext)
                || "ogg".equals(ext)
                || "m4a".equals(ext)
                || "aac".equals(ext);
    }

    private String convertDateTime(String value) {
        return AppTime.formatForTitle(value, "dd.MM.yyyy HH:mm");
    }
}
