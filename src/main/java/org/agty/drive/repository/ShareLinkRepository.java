package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.ShareLinkConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.ShareLinkDto;
import org.agty.drive.entity.AgdrvShareLink;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ShareLinkRepository {

    public ShareLinkDto findByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String query = """
                SELECT
                    s.id,
                    s.created_at,
                    s.updated_at,
                    s.created_by,
                    s.token,
                    s.resource_type,
                    s.resource_id,
                    s.title,
                    s.password_hash,
                    s.expires_at,
                    s.allow_download,
                    s.allow_preview,
                    s.is_enabled,
                    s.max_downloads,
                    s.download_count
                FROM public.agdrv_share_links s
                WHERE s.token = '%s'
                """.formatted(token.replace("'", "''"));

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : ShareLinkConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ShareLinkDto save(ShareLinkDto dto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            AgdrvShareLink saved = sql.sql().saveEntityWithCheck(ShareLinkConverter.dtoToEntity(dto));
            return saved == null || saved.getId() == null ? null : findByToken(dto.getToken());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ShareLinkDto findLatestByResource(String resourceType, Long resourceId) {
        if (resourceType == null || resourceId == null) {
            return null;
        }

        String query = """
                SELECT
                    s.id,
                    s.created_at,
                    s.updated_at,
                    s.created_by,
                    s.token,
                    s.resource_type,
                    s.resource_id,
                    s.title,
                    s.password_hash,
                    s.expires_at,
                    s.allow_download,
                    s.allow_preview,
                    s.is_enabled,
                    s.max_downloads,
                    s.download_count
                FROM public.agdrv_share_links s
                WHERE s.resource_type = '%s'
                  AND s.resource_id = %d
                  AND s.is_enabled = TRUE
                ORDER BY s.id DESC
                LIMIT 1
                """.formatted(resourceType.replace("'", "''"), resourceId);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : ShareLinkConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShareLinkDto> findLatestByResourceTypeAndIds(String resourceType, List<Long> resourceIds) {
        List<ShareLinkDto> result = new ArrayList<>();
        if (resourceType == null || resourceIds == null || resourceIds.isEmpty()) {
            return result;
        }

        String ids = resourceIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        String query = """
                SELECT
                    s.id,
                    s.created_at,
                    s.updated_at,
                    s.created_by,
                    s.token,
                    s.resource_type,
                    s.resource_id,
                    s.title,
                    s.password_hash,
                    s.expires_at,
                    s.allow_download,
                    s.allow_preview,
                    s.is_enabled,
                    s.max_downloads,
                    s.download_count
                FROM public.agdrv_share_links s
                JOIN (
                    SELECT resource_id, MAX(id) AS max_id
                    FROM public.agdrv_share_links
                    WHERE resource_type = '%s'
                      AND resource_id IN (%s)
                      AND is_enabled = TRUE
                    GROUP BY resource_id
                ) latest ON latest.max_id = s.id
                ORDER BY s.resource_id ASC
                """.formatted(resourceType.replace("'", "''"), ids);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(ShareLinkConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public ShareLinkDto findById(Long id) {
        if (id == null) {
            return null;
        }

        String query = """
                SELECT
                    s.id,
                    s.created_at,
                    s.updated_at,
                    s.created_by,
                    s.token,
                    s.resource_type,
                    s.resource_id,
                    s.title,
                    s.password_hash,
                    s.expires_at,
                    s.allow_download,
                    s.allow_preview,
                    s.is_enabled,
                    s.max_downloads,
                    s.download_count
                FROM public.agdrv_share_links s
                WHERE s.id = %d
                """.formatted(id);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : ShareLinkConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShareLinkDto> findActiveByCreator(Long createdBy) {
        List<ShareLinkDto> result = new ArrayList<>();
        if (createdBy == null) {
            return result;
        }

        String query = """
                SELECT
                    s.id,
                    s.created_at,
                    s.updated_at,
                    s.created_by,
                    s.token,
                    s.resource_type,
                    s.resource_id,
                    s.title,
                    s.password_hash,
                    s.expires_at,
                    s.allow_download,
                    s.allow_preview,
                    s.is_enabled,
                    s.max_downloads,
                    s.download_count
                FROM public.agdrv_share_links s
                WHERE s.created_by = %d
                  AND s.is_enabled = TRUE
                ORDER BY s.created_at DESC, s.id DESC
                """.formatted(createdBy);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(ShareLinkConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public int disableByResource(String resourceType, Long resourceId, Long createdBy) {
        if (resourceType == null || resourceId == null || createdBy == null) {
            return 0;
        }

        String query = """
                UPDATE public.agdrv_share_links
                SET is_enabled = FALSE,
                    updated_at = NOW()
                WHERE resource_type = ?
                  AND resource_id = ?
                  AND created_by = ?
                  AND is_enabled = TRUE
                """;

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow();
             var statement = sql.sql().getConnector().getConnection().prepareStatement(query)) {
            statement.setString(1, resourceType);
            statement.setLong(2, resourceId);
            statement.setLong(3, createdBy);
            int updated = statement.executeUpdate();
            if (!sql.sql().getConnector().getConnection().getAutoCommit()) {
                sql.sql().getConnector().getConnection().commit();
            }
            return updated;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int disableAllByResource(String resourceType, Long resourceId) {
        if (resourceType == null || resourceId == null) {
            return 0;
        }

        String query = """
                UPDATE public.agdrv_share_links
                SET is_enabled = FALSE,
                    updated_at = NOW()
                WHERE resource_type = ?
                  AND resource_id = ?
                  AND is_enabled = TRUE
                """;

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow();
             var statement = sql.sql().getConnector().getConnection().prepareStatement(query)) {
            statement.setString(1, resourceType);
            statement.setLong(2, resourceId);
            int updated = statement.executeUpdate();
            if (!sql.sql().getConnector().getConnection().getAutoCommit()) {
                sql.sql().getConnector().getConnection().commit();
            }
            return updated;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
