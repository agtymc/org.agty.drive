/*
 * Copyright 2026 Vladimir V
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
