package org.agty.drive.dto;

import lombok.Data;
import org.agty.drive.config.AppTime;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class ShareLinkDto {
    private Long id;
    private Long createdBy;
    private String token;
    private String resourceType;
    private Long resourceId;
    private String title;
    private String passwordHash;
    private String expiresAt;
    private Boolean allowDownload;
    private Boolean allowPreview;
    private Boolean isEnabled;
    private Long maxDownloads;
    private Long downloadCount;
    private String createdAt;
    private String updatedAt;

    public String getExpiresAtTitle() {
        if (expiresAt == null || expiresAt.isBlank()) {
            return "Без срока";
        }
        return AppTime.formatForTitle(expiresAt, "dd.MM.yyyy HH:mm");
    }

    public boolean isExpired() {
        LocalDateTime expiresDateTime = AppTime.parseDatabaseDateTime(expiresAt);
        return expiresDateTime != null && !expiresDateTime.isAfter(AppTime.now());
    }

    public boolean isWithoutExpiry() {
        return expiresAt == null || expiresAt.isBlank();
    }

    public boolean isExpiringSoon() {
        LocalDateTime expiresDateTime = AppTime.parseDatabaseDateTime(expiresAt);
        if (expiresDateTime == null || !expiresDateTime.isAfter(AppTime.now())) {
            return false;
        }
        return Duration.between(AppTime.now(), expiresDateTime).toHours() <= 24;
    }

    public String getExpiryStatusCode() {
        if (isWithoutExpiry()) {
            return "without_expiry";
        }
        if (isExpired()) {
            return "expired";
        }
        if (isExpiringSoon()) {
            return "expiring";
        }
        return "active";
    }

    public String getExpiryStatusTitle() {
        return switch (getExpiryStatusCode()) {
            case "without_expiry" -> "Без срока";
            case "expired" -> "Просрочено";
            case "expiring" -> "Срок скоро истечет";
            default -> "Активна";
        };
    }

    public String getRightsTitle() {
        boolean preview = Boolean.TRUE.equals(allowPreview);
        boolean download = Boolean.TRUE.equals(allowDownload);
        if (preview && download) {
            return "Просмотр и скачивание";
        }
        if (preview) {
            return "Только просмотр";
        }
        if (download) {
            return "Только скачивание";
        }
        return "Без прав";
    }
}
