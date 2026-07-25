package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.UsersRoleDictionaryConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.UsersRoleDictionaryDto;
import org.agty.drive.entity.UsersRoleDictionary;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

@Repository
public class UsersRoleDictionaryRepository {

    public List<UsersRoleDictionaryDto> findAll() {
        List<UsersRoleDictionaryDto> result = new LinkedList<>();

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(
                    Arguments.builder()
                            .setTable("public.agdrv_dic_users_roles")
                            .setOrderBy("align ASC, id ASC")
            )) != null) {
                result.add(UsersRoleDictionaryConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public UsersRoleDictionaryDto save(UsersRoleDictionaryDto dto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            UsersRoleDictionary saved = sql.sql().saveEntityWithCheck(UsersRoleDictionaryConverter.dtoToEntity(dto));
            if (saved == null || saved.getId() == null) {
                return null;
            }
            return findById(saved.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UsersRoleDictionaryDto findById(Long id) {
        if (id == null) {
            return null;
        }

        String query = """
                SELECT id, code, title, align, disabled
                FROM public.agdrv_dic_users_roles
                WHERE id = %d
                """.formatted(id);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : UsersRoleDictionaryConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
