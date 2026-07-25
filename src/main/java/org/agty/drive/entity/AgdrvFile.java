package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_files", schema = "public")
public class AgdrvFile {
    @Id
    private Long id;

    @Column(name = "created_at", skipIfNull = true)
    private String createdAt;

    @Column(name = "updated_at", skipIfNull = true)
    private String updatedAt;

    @Column(name = "deleted_at", skipIfNull = true)
    private String deletedAt;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "folder_id", skipIfNull = true)
    private Long folderId;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "storage_filename", skipIfNull = true)
    private String storageName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "extension", skipIfNull = true)
    private String extension;

    @Column(name = "file_size")
    private Long sizeBytes;

    @Column(name = "checksum", skipIfNull = true)
    private String checksumSha256;

    @Column(name = "description", skipIfNull = true)
    private String description;

    @Column(name = "preview_status")
    private String previewStatus;

    @Column(name = "is_image")
    private Boolean isImage;

    @Column(name = "is_video")
    private Boolean isVideo;
}
