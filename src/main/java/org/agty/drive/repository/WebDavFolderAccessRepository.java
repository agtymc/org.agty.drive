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
import org.agty.drive.converters.WebDavFolderAccessConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.WebDavFolderAccessDto;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class WebDavFolderAccessRepository {

    public WebDavFolderAccessDto save(WebDavFolderAccessDto dto) {
        if (dto == null) {
            return null;
        }
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            String query = """
                    INSERT INTO public.agdrv_folder_webdav_access (
                        owner_id,
                        folder_id,
                        access_token,
                        login_name,
                        password_hash,
                        allow_write,
                        is_enabled
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """;
            try (var statement = sql.sql().getConnector().getConnection().prepareStatement(query)) {
                statement.setLong(1, dto.getOwnerId());
                statement.setLong(2, dto.getFolderId());
                statement.setString(3, dto.getAccessToken());
                statement.setString(4, dto.getLoginName());
                statement.setString(5, dto.getPasswordHash());
                statement.setBoolean(6, Boolean.TRUE.equals(dto.getAllowWrite()));
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

    public WebDavFolderAccessDto findById(Long id) {
        if (id == null) {
            return null;
        }
        String query = selectBase() + """
                WHERE a.id = %d
                """.formatted(id);
        return findOne(query);
    }

    public WebDavFolderAccessDto findLatestByOwnerAndFolder(Long ownerId, Long folderId) {
        if (ownerId == null || folderId == null) {
            return null;
        }
        String query = selectBase() + """
                WHERE a.owner_id = %d
                  AND a.folder_id = %d
                ORDER BY a.id DESC
                LIMIT 1
                """.formatted(ownerId, folderId);
        return findOne(query);
    }

    public WebDavFolderAccessDto findByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String query = selectBase() + """
                WHERE a.access_token = '%s'
                  AND a.is_enabled = TRUE
                ORDER BY a.id DESC
                LIMIT 1
                """.formatted(token.replace("'", "''"));
        return findOne(query);
    }

    public List<WebDavFolderAccessDto> findLatestByOwner(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        String query = selectBase() + """
                JOIN (
                    SELECT folder_id, MAX(id) AS max_id
                    FROM public.agdrv_folder_webdav_access
                    WHERE owner_id = %d
                    GROUP BY folder_id
                ) latest ON latest.max_id = a.id
                WHERE a.owner_id = %d
                ORDER BY lower(f.name) ASC, a.id DESC
                """.formatted(ownerId, ownerId);
        return findMany(query);
    }

    public int disableByOwnerAndFolder(Long ownerId, Long folderId) {
        if (ownerId == null || folderId == null) {
            return 0;
        }
        String query = """
                UPDATE public.agdrv_folder_webdav_access
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

    public int deleteByOwnerAndFolder(Long ownerId, Long folderId) {
        if (ownerId == null || folderId == null) {
            return 0;
        }
        String query = """
                DELETE FROM public.agdrv_folder_webdav_access
                WHERE owner_id = ?
                  AND folder_id = ?
                """;

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow();
             var statement = sql.sql().getConnector().getConnection().prepareStatement(query)) {
            statement.setLong(1, ownerId);
            statement.setLong(2, folderId);
            int deleted = statement.executeUpdate();
            if (!sql.sql().getConnector().getConnection().getAutoCommit()) {
                sql.sql().getConnector().getConnection().commit();
            }
            return deleted;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private WebDavFolderAccessDto findOne(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            if (row == null) {
                return null;
            }
            WebDavFolderAccessDto dto = WebDavFolderAccessConverter.rowToDto(row);
            return dto.getId() == null ? null : dto;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<WebDavFolderAccessDto> findMany(String query) {
        List<WebDavFolderAccessDto> result = new ArrayList<>();
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(WebDavFolderAccessConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private String selectBase() {
        return """
                SELECT
                    a.id,
                    a.created_at,
                    a.updated_at,
                    a.owner_id,
                    a.folder_id,
                    a.access_token,
                    a.login_name,
                    a.password_hash,
                    a.allow_write,
                    a.is_enabled,
                    f.name AS folder_name,
                    f.path_key AS folder_path_key
                FROM public.agdrv_folder_webdav_access a
                JOIN public.agdrv_folders f ON f.id = a.folder_id
                """;
    }
}
