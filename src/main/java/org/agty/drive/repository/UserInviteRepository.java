package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.UserInviteConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.UserInviteDto;
import org.agty.drive.entity.AgdrvUserInvite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserInviteRepository {

    private static final Logger log = LoggerFactory.getLogger(UserInviteRepository.class);

    public UserInviteDto save(UserInviteDto dto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            AgdrvUserInvite saved = sql.sql().saveEntityWithCheck(UserInviteConverter.dtoToEntity(dto));
            if (saved == null || saved.getId() == null) {
                log.error("Failed to save user invite. login={}, errors={}", dto.getLogin(), sql.sql().getErrors());
                return null;
            }
            return findById(saved.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UserInviteDto findById(Long id) {
        if (id == null) {
            return null;
        }
        String query = baseSelect() + " WHERE i.id = " + id;
        return fetchOne(query);
    }

    public UserInviteDto findByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String query = baseSelect() + " WHERE i.token = '%s'".formatted(token.replace("'", "''"));
        return fetchOne(query);
    }

    public List<UserInviteDto> findAll() {
        List<UserInviteDto> result = new ArrayList<>();
        String query = baseSelect() + " ORDER BY i.id DESC";
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(UserInviteConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private UserInviteDto fetchOne(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : UserInviteConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String baseSelect() {
        return """
                SELECT
                    i.id,
                    i.created_at,
                    i.updated_at,
                    i.created_by,
                    i.token,
                    i.login,
                    i.email,
                    i.display_name,
                    i.role_code,
                    role_dic.title AS role_title,
                    i.status_code,
                    status_dic.title AS status_title,
                    i.storage_quota_bytes,
                    i.expires_at,
                    i.is_enabled,
                    i.used_at,
                    i.invited_user_id
                FROM public.agdrv_user_invites i
                LEFT JOIN public.agdrv_dic_users_roles role_dic ON role_dic.code = i.role_code
                LEFT JOIN public.agdrv_dic_users_statuses status_dic ON status_dic.code = i.status_code
                """;
    }
}
