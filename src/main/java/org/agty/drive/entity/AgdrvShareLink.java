package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_share_links", schema = "public")
public class AgdrvShareLink {
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

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "title", skipIfNull = true)
    private String title;

    @Column(name = "password_hash", skipIfNull = true)
    private String passwordHash;

    @Column(name = "expires_at", skipIfNull = true)
    private String expiresAt;

    @Column(name = "allow_download")
    private Boolean allowDownload;

    @Column(name = "allow_preview")
    private Boolean allowPreview;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

    @Column(name = "max_downloads", skipIfNull = true)
    private Long maxDownloads;

    @Column(name = "download_count")
    private Long downloadCount;
}
