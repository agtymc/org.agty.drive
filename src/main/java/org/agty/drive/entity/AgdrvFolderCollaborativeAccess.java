package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_folder_collaborative_access", schema = "public")
public class AgdrvFolderCollaborativeAccess {
    @Id
    private Long id;

    @Column(name = "created_at", skipIfNull = true)
    private String createdAt;

    @Column(name = "updated_at", skipIfNull = true)
    private String updatedAt;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "password_hash", skipIfNull = true)
    private String passwordHash;

    @Column(name = "allow_write")
    private Boolean allowWrite;

    @Column(name = "allow_delete")
    private Boolean allowDelete;

    @Column(name = "is_enabled")
    private Boolean isEnabled;
}
