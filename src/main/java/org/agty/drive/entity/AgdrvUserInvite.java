package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_user_invites", schema = "public")
public class AgdrvUserInvite {
    @Id
    private Long id;

    @Column(name = "created_at", skipIfNull = true)
    private String createdAt;

    @Column(name = "updated_at", skipIfNull = true)
    private String updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "token")
    private String token;

    @Column(name = "login")
    private String login;

    @Column(name = "email", skipIfNull = true)
    private String email;

    @Column(name = "display_name", skipIfNull = true)
    private String displayName;

    @Column(name = "role_code")
    private String roleCode;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "storage_quota_bytes")
    private Long storageQuotaBytes;

    @Column(name = "expires_at", skipIfNull = true)
    private String expiresAt;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "used_at", skipIfNull = true)
    private String usedAt;

    @Column(name = "invited_user_id", skipIfNull = true)
    private Long invitedUserId;
}
