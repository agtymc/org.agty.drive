package org.agty.drive.converters;

import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.config.AppTime;
import org.agty.drive.dto.AppSettingDto;
import org.agty.drive.entity.AgdrvSetting;

public final class AppSettingConverter {

    private AppSettingConverter() {
    }

    public static AppSettingDto rowToDto(SqlRow row) {
        AppSettingDto dto = new AppSettingDto();
        dto.setId(row.getLong("id"));
        dto.setCreatedAt(row.getString("created_at"));
        dto.setUpdatedAt(row.getString("updated_at"));
        dto.setSettingKey(row.getString("setting_key"));
        dto.setSettingValue(row.getString("setting_value"));
        dto.setUpdatedBy(row.getLong("updated_by"));
        return dto;
    }

    public static AgdrvSetting dtoToEntity(AppSettingDto dto) {
        AgdrvSetting entity = new AgdrvSetting();
        entity.setId(dto.getId());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(AppTime.nowForDatabase());
        entity.setSettingKey(dto.getSettingKey());
        entity.setSettingValue(dto.getSettingValue());
        entity.setUpdatedBy(dto.getUpdatedBy());
        return entity;
    }
}
