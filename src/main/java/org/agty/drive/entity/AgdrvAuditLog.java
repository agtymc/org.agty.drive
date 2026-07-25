package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_audit_log", schema = "public")
public class AgdrvAuditLog {
    @Id
    private Long id;

    @Column(name = "created_at", skipIfNull = true)
    private String createdAt;

    @Column(name = "actor_user_id", skipIfNull = true)
    private Long actorUserId;

    @Column(name = "action_code")
    private String actionCode;

    @Column(name = "resource_type", skipIfNull = true)
    private String resourceType;

    @Column(name = "resource_id", skipIfNull = true)
    private Long resourceId;

    @Column(name = "details", skipIfNull = true)
    private String details;
}
