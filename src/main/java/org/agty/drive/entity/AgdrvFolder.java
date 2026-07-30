package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_folders", schema = "public")
public class AgdrvFolder {
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

    @Column(name = "parent_id", skipIfNull = true)
    private Long parentId;

    @Column(name = "name")
    private String name;

    @Column(name = "path_key")
    private String pathKey;

    @Column(name = "description", skipIfNull = true)
    private String description;

    @Column(name = "expires_at", skipIfNull = true)
    private String expiresAt;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
