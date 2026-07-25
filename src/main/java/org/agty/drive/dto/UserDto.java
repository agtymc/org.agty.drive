package org.agty.drive.dto;

import lombok.Data;
import org.agty.utils.AgtyUtils;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String login;
    private String email;
    private String passwordHash;
    private String roleCode;
    private String roleTitle;
    private String statusCode;
    private String statusTitle;
    private String firstName;
    private String lastName;
    private String middleName;
    private String displayName;
    private Long createdBy;
    private String createdAt;
    private String updatedAt;
    private String lastLoginAt;
    private Long storageQuotaBytes;
    private Boolean twoFactorEmailEnabled;
    private Boolean twoFactorTotpEnabled;
    private String twoFactorTotpSecret;
    private String twoFactorTotpCreatedAt;
    private String twoFactorEmailCodeHash;
    private String twoFactorEmailCodeExpiresAt;

    public String getStorageQuotaTitle() {
        if (storageQuotaBytes == null || storageQuotaBytes <= 0) {
            return "0 Б";
        }
        return AgtyUtils.filesizeToTitle(storageQuotaBytes, "ru");
    }

    public Long getStorageQuotaMb() {
        if (storageQuotaBytes == null || storageQuotaBytes <= 0) {
            return 0L;
        }
        return storageQuotaBytes / (1024L * 1024L);
    }
}
