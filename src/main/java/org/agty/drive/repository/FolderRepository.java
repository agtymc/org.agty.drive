package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.FolderConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.FolderDto;
import org.agty.drive.entity.AgdrvFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FolderRepository {

    private static final Logger log = LoggerFactory.getLogger(FolderRepository.class);

    public List<FolderDto> findRootFoldersByOwnerId(Long ownerId) {
        return findByOwnerIdAndParentId(ownerId, null);
    }

    public List<FolderDto> findAllByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }

        String query = selectBase() + """
                WHERE owner_id = %d
                  AND deleted_at IS NULL
                ORDER BY path_key ASC, sort_order ASC, name ASC
                """.formatted(ownerId);

        return findMany(query);
    }

    public List<FolderDto> findByOwnerIdAndParentId(Long ownerId, Long parentId) {
        if (ownerId == null) {
            return List.of();
        }

        String parentCondition = parentId == null ? "parent_id IS NULL" : "parent_id = %d".formatted(parentId);
        String query = selectBase() + """
                WHERE owner_id = %d
                  AND %s
                  AND deleted_at IS NULL
                ORDER BY sort_order ASC, name ASC
                """.formatted(ownerId, parentCondition);

        return findMany(query);
    }

    public List<FolderDto> searchByOwnerId(Long ownerId,
                                           String query,
                                           Long currentFolderId,
                                           String currentFolderPath,
                                           String scope) {
        return searchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope, "name_asc", 0, Integer.MAX_VALUE);
    }

    public long countSearchByOwnerId(Long ownerId,
                                     String query,
                                     Long currentFolderId,
                                     String currentFolderPath,
                                     String scope) {
        if (ownerId == null) {
            return 0L;
        }

        String queryCondition = buildFolderQueryCondition(query);
        String scopeCondition = buildFolderScopeCondition(currentFolderId, currentFolderPath, scope);
        String sqlQuery = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_folders
                WHERE owner_id = %d
                  AND deleted_at IS NULL
                  AND %s
                  %s
                """.formatted(ownerId, scopeCondition, queryCondition);

        return fetchCount(sqlQuery);
    }

    public List<FolderDto> searchByOwnerId(Long ownerId,
                                           String query,
                                           Long currentFolderId,
                                           String currentFolderPath,
                                           String scope,
                                           String sortMode,
                                           int offset,
                                           int limit) {
        if (ownerId == null || limit <= 0) {
            return List.of();
        }

        String queryCondition = buildFolderQueryCondition(query);
        String scopeCondition = buildFolderScopeCondition(currentFolderId, currentFolderPath, scope);
        String orderBy = buildFolderOrderBy(sortMode);
        String sqlQuery = selectBase() + """
                WHERE owner_id = %d
                  AND deleted_at IS NULL
                  AND %s
                  %s
                ORDER BY %s
                OFFSET %d
                LIMIT %d
                """.formatted(ownerId, scopeCondition, queryCondition, orderBy, Math.max(0, offset), limit);

        return findMany(sqlQuery);
    }

    public long countByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return 0L;
        }

        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_folders
                WHERE owner_id = %d
                  AND deleted_at IS NULL
                """.formatted(ownerId);

        return fetchCount(query);
    }

    public long countAll() {
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_folders
                WHERE deleted_at IS NULL
                """;

        return fetchCount(query);
    }

    public FolderDto save(FolderDto folderDto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            AgdrvFolder saved = sql.sql().saveEntityWithCheck(FolderConverter.dtoToEntity(folderDto));
            if (saved == null || saved.getId() == null) {
                log.error("Failed to save agdrv folder. ownerId={}, name={}, pathKey={}, errors={}",
                        folderDto.getOwnerId(), folderDto.getName(), folderDto.getPathKey(), sql.sql().getErrors());
                return null;
            }

            return findById(saved.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public FolderDto findById(Long id) {
        return findByIdAndOwnerId(id, null);
    }

    public FolderDto findByIdAndOwnerId(Long id, Long ownerId) {
        if (id == null) {
            return null;
        }

        String ownerCondition = ownerId == null ? "" : " AND owner_id = %d".formatted(ownerId);
        String query = selectBase() + """
                WHERE id = %d
                %s
                """.formatted(id, ownerCondition);

        return findOne(query);
    }

    public boolean existsByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name) {
        return existsByOwnerIdAndParentIdAndName(ownerId, parentId, name, null);
    }

    public boolean existsByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name, Long excludeId) {
        if (ownerId == null || name == null || name.isBlank()) {
            return false;
        }

        String escapedName = name.trim().replace("'", "''");
        String parentCondition = parentId == null ? "parent_id IS NULL" : "parent_id = %d".formatted(parentId);
        String excludeCondition = excludeId == null ? "" : " AND id <> %d".formatted(excludeId);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_folders
                WHERE owner_id = %d
                  AND %s
                  AND deleted_at IS NULL
                  AND lower(name) = lower('%s')
                  %s
                """.formatted(ownerId, parentCondition, escapedName, excludeCondition);

        return fetchCount(query) > 0;
    }

    public List<FolderDto> findExpiredActiveFolders() {
        String query = selectBase() + """
                WHERE deleted_at IS NULL
                  AND expires_at IS NOT NULL
                  AND expires_at <= NOW()
                ORDER BY path_key ASC, id ASC
                """;

        return findMany(query);
    }

    private List<FolderDto> findMany(String query) {
        List<FolderDto> result = new ArrayList<>();
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(FolderConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private FolderDto findOne(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : FolderConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private long fetchCount(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total == null ? 0L : total;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildFolderQueryCondition(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return " AND name ILIKE '%" + escapeSqlLiteral(query.trim()) + "%'";
    }

    private String buildFolderScopeCondition(Long currentFolderId, String currentFolderPath, String scope) {
        if ("all".equalsIgnoreCase(scope)) {
            return "1 = 1";
        }
        if ("tree".equalsIgnoreCase(scope)) {
            if (currentFolderId == null || currentFolderPath == null || currentFolderPath.isBlank()) {
                return "1 = 1";
            }
            return "path_key LIKE '" + escapeSqlLiteral(currentFolderPath) + "/%'";
        }
        return currentFolderId == null
                ? "parent_id IS NULL"
                : "parent_id = " + currentFolderId;
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private String buildFolderOrderBy(String sortMode) {
        if ("name_desc".equalsIgnoreCase(sortMode)) {
            return "lower(name) DESC, id DESC";
        }
        if ("date_newest".equalsIgnoreCase(sortMode)) {
            return "created_at DESC, lower(name) ASC, id DESC";
        }
        if ("date_oldest".equalsIgnoreCase(sortMode)) {
            return "created_at ASC, lower(name) ASC, id ASC";
        }
        return "lower(name) ASC, id ASC";
    }

    private String selectBase() {
        return """
                SELECT
                    id,
                    created_at,
                    updated_at,
                    deleted_at,
                    owner_id,
                    parent_id,
                    name,
                    path_key,
                    description,
                    expires_at,
                    sort_order
                FROM public.agdrv_folders
                """;
    }
}
