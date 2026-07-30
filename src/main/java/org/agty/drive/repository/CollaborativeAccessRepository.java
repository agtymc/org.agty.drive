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
import org.agty.drive.converters.CollaborativeAccessConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.CollaborativeAccessDto;
import org.agty.drive.entity.AgdrvFolderCollaborativeAccess;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CollaborativeAccessRepository {

    public CollaborativeAccessDto save(CollaborativeAccessDto dto) {
        if (dto == null) {
            return null;
        }
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            String query = """
                    INSERT INTO public.agdrv_folder_collaborative_access (
                        owner_id,
                        folder_id,
                        target_user_id,
                        password_hash,
                        allow_write,
                        allow_delete,
                        is_enabled
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """;
            try (var statement = sql.sql().getConnector().getConnection().prepareStatement(query)) {
                statement.setLong(1, dto.getOwnerId());
                statement.setLong(2, dto.getFolderId());
                statement.setLong(3, dto.getTargetUserId());
                statement.setString(4, dto.getPasswordHash());
                statement.setBoolean(5, Boolean.TRUE.equals(dto.getAllowWrite()));
                statement.setBoolean(6, Boolean.TRUE.equals(dto.getAllowDelete()));
                statement.setBoolean(7, !Boolean.FALSE.equals(dto.getIsEnabled()));
                try (var resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    long id = resultSet.getLong(1);
                    if (!sql.sql().getConnector().getConnection().getAutoCommit()) {
                        sql.sql().getConnector().getConnection().commit();
                    }
                    return findById(id);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CollaborativeAccessDto findById(Long id) {
        if (id == null) {
            return null;
        }
        String query = baseSelect() + """
                WHERE access.id = %d
                """.formatted(id);
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : CollaborativeAccessConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<CollaborativeAccessDto> findActiveByOwner(Long ownerId) {
        return findMany("""
                WHERE access.owner_id = %d
                  AND access.is_enabled = TRUE
                ORDER BY folder.name ASC, target_user.login ASC
                """.formatted(ownerId));
    }

    public List<CollaborativeAccessDto> findActiveByOwnerAndFolder(Long ownerId, Long folderId) {
        return findMany("""
                WHERE access.owner_id = %d
                  AND access.folder_id = %d
                  AND access.is_enabled = TRUE
                ORDER BY target_user.login ASC
                """.formatted(ownerId, folderId));
    }

    public List<CollaborativeAccessDto> findActiveByTargetUser(Long userId) {
        return findMany("""
                WHERE access.target_user_id = %d
                  AND access.is_enabled = TRUE
                ORDER BY owner_user.login ASC, folder.name ASC
                """.formatted(userId));
    }

    public int disableByOwnerAndFolder(Long ownerId, Long folderId) {
        if (ownerId == null || folderId == null) {
            return 0;
        }
        String query = """
                UPDATE public.agdrv_folder_collaborative_access
                SET is_enabled = FALSE,
                    updated_at = NOW()
                WHERE owner_id = ?
                  AND folder_id = ?
                  AND is_enabled = TRUE
                """;
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow();
             var statement = sql.sql().getConnector().getConnection().prepareStatement(query)) {
            statement.setLong(1, ownerId);
            statement.setLong(2, folderId);
            int updated = statement.executeUpdate();
            if (!sql.sql().getConnector().getConnection().getAutoCommit()) {
                sql.sql().getConnector().getConnection().commit();
            }
            return updated;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int disableByFolder(Long folderId) {
        if (folderId == null) {
            return 0;
        }
        String query = """
                UPDATE public.agdrv_folder_collaborative_access
                SET is_enabled = FALSE,
                    updated_at = NOW()
                WHERE folder_id = ?
                  AND is_enabled = TRUE
                """;
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow();
             var statement = sql.sql().getConnector().getConnection().prepareStatement(query)) {
            statement.setLong(1, folderId);
            int updated = statement.executeUpdate();
            if (!sql.sql().getConnector().getConnection().getAutoCommit()) {
                sql.sql().getConnector().getConnection().commit();
            }
            return updated;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<CollaborativeAccessDto> findMany(String whereClause) {
        List<CollaborativeAccessDto> result = new ArrayList<>();
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(baseSelect() + whereClause))) != null) {
                result.add(CollaborativeAccessConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private String baseSelect() {
        return """
                SELECT
                    access.id,
                    access.created_at,
                    access.updated_at,
                    access.owner_id,
                    access.folder_id,
                    access.target_user_id,
                    access.password_hash,
                    access.allow_write,
                    access.allow_delete,
                    access.is_enabled,
                    owner_user.login AS owner_login,
                    owner_user.display_name AS owner_display_name,
                    folder.name AS folder_name,
                    folder.path_key AS folder_path_key,
                    target_user.login AS target_user_login,
                    target_user.display_name AS target_user_display_name
                FROM public.agdrv_folder_collaborative_access access
                JOIN public.agdrv_users owner_user ON owner_user.id = access.owner_id
                JOIN public.agdrv_folders folder ON folder.id = access.folder_id
                JOIN public.agdrv_users target_user ON target_user.id = access.target_user_id
                """;
    }
}
