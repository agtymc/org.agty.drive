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
import org.agty.drive.dto.UsersRoleDictionaryDto;
import org.agty.drive.entity.UsersRoleDictionary;

public final class UsersRoleDictionaryConverter {

    private UsersRoleDictionaryConverter() {
    }

    public static UsersRoleDictionaryDto rowToDto(SqlRow row) {
        UsersRoleDictionaryDto dto = new UsersRoleDictionaryDto();
        dto.setId(row.getLong("id"));
        dto.setCode(row.getString("code"));
        dto.setTitle(row.getDstring("title"));
        dto.setAlign(row.getInt("align"));
        dto.setDisabled(row.getBoolean("disabled"));
        return dto;
    }

    public static UsersRoleDictionary dtoToEntity(UsersRoleDictionaryDto dto) {
        UsersRoleDictionary entity = new UsersRoleDictionary();
        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setTitle(dto.getTitle());
        entity.setAlign(dto.getAlign() == null ? 0 : dto.getAlign());
        entity.setDisabled(dto.getDisabled());
        return entity;
    }
}
