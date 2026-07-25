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
