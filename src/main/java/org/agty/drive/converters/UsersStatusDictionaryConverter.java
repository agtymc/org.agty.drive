package org.agty.drive.converters;

import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.dto.UsersStatusDictionaryDto;
import org.agty.drive.entity.UsersStatusDictionary;

public final class UsersStatusDictionaryConverter {

    private UsersStatusDictionaryConverter() {
    }

    public static UsersStatusDictionaryDto rowToDto(SqlRow row) {
        UsersStatusDictionaryDto dto = new UsersStatusDictionaryDto();
        dto.setId(row.getLong("id"));
        dto.setCode(row.getString("code"));
        dto.setTitle(row.getDstring("title"));
        dto.setAlign(row.getInt("align"));
        dto.setDisabled(row.getBoolean("disabled"));
        return dto;
    }

    public static UsersStatusDictionary dtoToEntity(UsersStatusDictionaryDto dto) {
        UsersStatusDictionary entity = new UsersStatusDictionary();
        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setTitle(dto.getTitle());
        entity.setAlign(dto.getAlign() == null ? 0 : dto.getAlign());
        entity.setDisabled(dto.getDisabled());
        return entity;
    }
}
