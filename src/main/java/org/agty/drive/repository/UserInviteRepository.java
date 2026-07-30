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

package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.UserInviteConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.config.AppTime;
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

    public boolean existsActiveByLogin(String login, Long excludeId) {
        if (login == null || login.isBlank()) {
            return false;
        }
        String excludeCondition = excludeId == null ? "" : " AND i.id <> %d".formatted(excludeId);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_user_invites i
                WHERE lower(i.login) = lower('%s')
                  AND i.is_enabled = TRUE
                  AND i.used_at IS NULL
                %s
                """.formatted(login.trim().replace("'", "''"), excludeCondition);
        return countByQuery(query) > 0;
    }

    public boolean existsActiveByEmail(String email, Long excludeId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String excludeCondition = excludeId == null ? "" : " AND i.id <> %d".formatted(excludeId);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_user_invites i
                WHERE lower(i.email) = lower('%s')
                  AND i.is_enabled = TRUE
                  AND i.used_at IS NULL
                %s
                """.formatted(email.trim().replace("'", "''"), excludeCondition);
        return countByQuery(query) > 0;
    }

    public long countAll() {
        return countByQuery("SELECT COUNT(*) AS total FROM public.agdrv_user_invites");
    }

    public long countActive() {
        return countByQuery("""
                SELECT COUNT(*) AS total
                FROM public.agdrv_user_invites
                WHERE is_enabled = TRUE
                  AND used_at IS NULL
                """);
    }

    public long countUsed() {
        return countByQuery("""
                SELECT COUNT(*) AS total
                FROM public.agdrv_user_invites
                WHERE used_at IS NOT NULL
                """);
    }

    public boolean disable(Long inviteId) {
        if (inviteId == null) {
            return false;
        }
        String query = """
                UPDATE public.agdrv_user_invites
                SET is_enabled = FALSE,
                    updated_at = '%s'
                WHERE id = %d
                """.formatted(AppTime.nowForDatabase(), inviteId);
        return executeUpdate(query);
    }

    public boolean markUsed(Long inviteId, Long invitedUserId) {
        if (inviteId == null || invitedUserId == null) {
            return false;
        }
        String now = AppTime.nowForDatabase();
        String query = """
                UPDATE public.agdrv_user_invites
                SET is_enabled = FALSE,
                    used_at = '%s',
                    invited_user_id = %d,
                    updated_at = '%s'
                WHERE id = %d
                """.formatted(now, invitedUserId, now, inviteId);
        return executeUpdate(query);
    }

    private UserInviteDto fetchOne(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : UserInviteConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private long countByQuery(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total == null ? 0L : total;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean executeUpdate(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            return sql.sql().executeUpdate(query) > 0;
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
