package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.UsersStatusDictionaryConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.UsersStatusDictionaryDto;
import org.agty.drive.entity.UsersStatusDictionary;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

@Repository
public class UsersStatusDictionaryRepository {

    public List<UsersStatusDictionaryDto> findAll() {
        List<UsersStatusDictionaryDto> result = new LinkedList<>();

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(
                    Arguments.builder()
                            .setTable("public.agdrv_dic_users_statuses")
                            .setOrderBy("align ASC, id ASC")
            )) != null) {
                result.add(UsersStatusDictionaryConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public UsersStatusDictionaryDto save(UsersStatusDictionaryDto dto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            UsersStatusDictionary saved = sql.sql().saveEntityWithCheck(UsersStatusDictionaryConverter.dtoToEntity(dto));
            if (saved == null || saved.getId() == null) {
                return null;
            }
            return findById(saved.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UsersStatusDictionaryDto findById(Long id) {
        if (id == null) {
            return null;
        }

        String query = """
                SELECT id, code, title, align, disabled
                FROM public.agdrv_dic_users_statuses
                WHERE id = %d
                """.formatted(id);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : UsersStatusDictionaryConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
