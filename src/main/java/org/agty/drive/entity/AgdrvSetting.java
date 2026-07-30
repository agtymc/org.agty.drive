package org.agty.drive.entity;

import lombok.Data;
import org.agty.agtysql.model.annotations.Column;
import org.agty.agtysql.model.annotations.Entity;
import org.agty.agtysql.model.annotations.Id;
import org.agty.agtysql.model.annotations.Table;

@Data
@Entity
@Table(name = "agdrv_settings", schema = "public")
public class AgdrvSetting {
    @Id
    private Long id;

    @Column(name = "created_at", skipIfNull = true)
    private String createdAt;

    @Column(name = "updated_at", skipIfNull = true)
    private String updatedAt;

    @Column(name = "setting_key")
    private String settingKey;

    @Column(name = "setting_value", skipIfNull = true)
    private String settingValue;

    @Column(name = "updated_by", skipIfNull = true)
    private Long updatedBy;
}
