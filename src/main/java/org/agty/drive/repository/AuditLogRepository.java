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
import org.agty.drive.converters.AuditLogConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.AuditLogDto;
import org.agty.drive.entity.AgdrvAuditLog;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogRepository {

    public AuditLogDto save(AuditLogDto dto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            AgdrvAuditLog saved = sql.sql().saveEntityWithCheck(AuditLogConverter.dtoToEntity(dto));
            if (saved == null || saved.getId() == null) {
                return null;
            }
            return findById(saved.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public AuditLogDto findById(Long id) {
        if (id == null) {
            return null;
        }
        String query = """
                SELECT
                    log.id,
                    log.created_at,
                    log.actor_user_id,
                    actor.login AS actor_login,
                    log.action_code,
                    log.resource_type,
                    log.resource_id,
                    log.details
                FROM public.agdrv_audit_log log
                LEFT JOIN public.agdrv_users actor ON actor.id = log.actor_user_id
                WHERE log.id = %d
                """.formatted(id);
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : AuditLogConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AuditLogDto> findRecent(int limit) {
        List<AuditLogDto> result = new ArrayList<>();
        String query = """
                SELECT
                    log.id,
                    log.created_at,
                    log.actor_user_id,
                    actor.login AS actor_login,
                    log.action_code,
                    log.resource_type,
                    log.resource_id,
                    log.details
                FROM public.agdrv_audit_log log
                LEFT JOIN public.agdrv_users actor ON actor.id = log.actor_user_id
                ORDER BY log.created_at DESC, log.id DESC
                LIMIT %d
                """.formatted(Math.max(1, limit));
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(AuditLogConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public long countAll() {
        return countFiltered(null, null, null, null, null);
    }

    public long countFiltered(String createdDate,
                              String actorLogin,
                              String actionCode,
                              String resourceQuery,
                              String details) {
        String filters = buildFilters(createdDate, actorLogin, actionCode, resourceQuery, details);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_audit_log log
                LEFT JOIN public.agdrv_users actor ON actor.id = log.actor_user_id
                WHERE 1 = 1
                %s
                """.formatted(filters);
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total == null ? 0L : total;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AuditLogDto> findPage(String sortMode, int offset, int limit) {
        return findPage(sortMode, offset, limit, null, null, null, null, null);
    }

    public List<AuditLogDto> findPage(String sortMode,
                                      int offset,
                                      int limit,
                                      String createdDate,
                                      String actorLogin,
                                      String actionCode,
                                      String resourceQuery,
                                      String details) {
        List<AuditLogDto> result = new ArrayList<>();
        String orderBy = switch (sortMode == null ? "" : sortMode.trim().toLowerCase()) {
            case "date_asc" -> "log.created_at ASC, log.id ASC";
            default -> "log.created_at DESC, log.id DESC";
        };
        String filters = buildFilters(createdDate, actorLogin, actionCode, resourceQuery, details);
        String query = """
                SELECT
                    log.id,
                    log.created_at,
                    log.actor_user_id,
                    actor.login AS actor_login,
                    log.action_code,
                    log.resource_type,
                    log.resource_id,
                    log.details
                FROM public.agdrv_audit_log log
                LEFT JOIN public.agdrv_users actor ON actor.id = log.actor_user_id
                WHERE 1 = 1
                %s
                ORDER BY %s
                OFFSET %d
                LIMIT %d
                """.formatted(filters, orderBy, Math.max(0, offset), Math.max(1, limit));
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(AuditLogConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private String buildFilters(String createdDate,
                                String actorLogin,
                                String actionCode,
                                String resourceQuery,
                                String details) {
        StringBuilder filters = new StringBuilder();

        if (createdDate != null && createdDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            filters.append("\n  AND CAST(log.created_at AS DATE) = DATE '")
                    .append(createdDate)
                    .append("'");
        }

        appendIlike(filters, "actor.login", actorLogin);
        appendIlike(filters, "log.action_code", actionCode);
        appendResourceFilter(filters, resourceQuery);
        appendIlike(filters, "log.details", details);

        return filters.toString();
    }

    private void appendIlike(StringBuilder filters, String column, String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return;
        }
        filters.append("\n  AND ")
                .append(column)
                .append(" ILIKE '%")
                .append(escapeSql(normalized))
                .append("%'");
    }

    private void appendResourceFilter(StringBuilder filters, String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return;
        }
        String escaped = escapeSql(normalized);
        filters.append("\n  AND (")
                .append("log.resource_type ILIKE '%").append(escaped).append("%'")
                .append(" OR CAST(log.resource_id AS TEXT) ILIKE '%").append(escaped).append("%'")
                .append(")");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }
}
