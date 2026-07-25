package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_users", schema = "public")
public class AgdrvUser {
    @Id
    private Long id;

    @Column(name = "created_at", skipIfNull = true)
    private String createdAt;

    @Column(name = "updated_at", skipIfNull = true)
    private String updatedAt;

    @Column(name = "login")
    private String login;

    @Column(name = "email", skipIfNull = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "role_code")
    private String roleCode;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "first_name", skipIfNull = true)
    private String firstName;

    @Column(name = "last_name", skipIfNull = true)
    private String lastName;

    @Column(name = "middle_name", skipIfNull = true)
    private String middleName;

    @Column(name = "display_name", skipIfNull = true)
    private String displayName;

    @Column(name = "created_by", skipIfNull = true)
    private Long createdBy;

    @Column(name = "last_login_at", skipIfNull = true)
    private String lastLoginAt;

    @Column(name = "storage_quota_bytes")
    private Long storageQuotaBytes;

    @Column(name = "two_factor_email_enabled")
    private Boolean twoFactorEmailEnabled;

    @Column(name = "two_factor_totp_enabled")
    private Boolean twoFactorTotpEnabled;

    @Column(name = "two_factor_totp_secret", skipIfNull = true)
    private String twoFactorTotpSecret;

    @Column(name = "two_factor_totp_created_at", skipIfNull = true)
    private String twoFactorTotpCreatedAt;

    @Column(name = "two_factor_email_code_hash", skipIfNull = true)
    private String twoFactorEmailCodeHash;

    @Column(name = "two_factor_email_code_expires_at", skipIfNull = true)
    private String twoFactorEmailCodeExpiresAt;
}
