package org.agty.drive.repository;

import org.agty.agtysql.data.Arguments;
import org.agty.agtysql.interfaces.SqlRow;
import org.agty.drive.converters.FileItemConverter;
import org.agty.drive.dao.AgtySQLPool;
import org.agty.drive.dao.ConnectionPool;
import org.agty.drive.dto.FileItemDto;
import org.agty.drive.entity.AgdrvFile;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FileRepository {

    public FileRepository() {
    }

    public List<FileItemDto> findAllByOwnerId(Long ownerId) {
        List<FileItemDto> result = new ArrayList<>();
        if (ownerId == null) {
            return result;
        }

        String query = selectBase() + """
                WHERE f.owner_id = %d
                  AND f.deleted_at IS NULL
                ORDER BY f.created_at DESC, f.id DESC
                """.formatted(ownerId);

        return findMany(query);
    }

    public List<FileItemDto> findByOwnerIdAndFolderId(Long ownerId, Long folderId) {
        if (ownerId == null) {
            return List.of();
        }

        String folderCondition = folderId == null ? "f.folder_id IS NULL" : "f.folder_id = %d".formatted(folderId);
        String query = selectBase() + """
                WHERE f.owner_id = %d
                  AND %s
                  AND f.deleted_at IS NULL
                ORDER BY f.created_at DESC, f.id DESC
                """.formatted(ownerId, folderCondition);

        return findMany(query);
    }

    public List<FileItemDto> searchByOwnerId(Long ownerId,
                                             String query,
                                             Long currentFolderId,
                                             String currentFolderPath,
                                             String scope) {
        return searchByOwnerId(ownerId, query, currentFolderId, currentFolderPath, scope, "name_asc", 0, Integer.MAX_VALUE);
    }

    public long countAll() {
        String sqlQuery = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_files f
                WHERE f.deleted_at IS NULL
                """;

        return fetchCount(sqlQuery);
    }

    public long countByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return 0L;
        }

        String sqlQuery = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_files f
                WHERE f.owner_id = %d
                  AND f.deleted_at IS NULL
                """.formatted(ownerId);

        return fetchCount(sqlQuery);
    }

    public long countSearchByOwnerId(Long ownerId,
                                     String query,
                                     Long currentFolderId,
                                     String currentFolderPath,
                                     String scope) {
        if (ownerId == null) {
            return 0L;
        }

        String queryCondition = buildFileQueryCondition(query);
        String scopeCondition = buildFileScopeCondition(currentFolderId, currentFolderPath, scope);
        String sqlQuery = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                WHERE f.owner_id = %d
                  AND f.deleted_at IS NULL
                  AND %s
                  %s
                """.formatted(ownerId, scopeCondition, queryCondition);

        return fetchCount(sqlQuery);
    }

    public List<FileItemDto> searchByOwnerId(Long ownerId,
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

        String queryCondition = buildFileQueryCondition(query);
        String scopeCondition = buildFileScopeCondition(currentFolderId, currentFolderPath, scope);
        String orderBy = buildFileOrderBy(sortMode);
        String sqlQuery = selectBase() + """
                WHERE f.owner_id = %d
                  AND f.deleted_at IS NULL
                  AND %s
                  %s
                ORDER BY %s
                OFFSET %d
                LIMIT %d
                """.formatted(ownerId, scopeCondition, queryCondition, orderBy, Math.max(0, offset), limit);

        return findMany(sqlQuery);
    }

    public FileItemDto findByIdAndOwnerId(Long id, Long ownerId) {
        if (id == null || ownerId == null) {
            return null;
        }

        String query = selectBase() + """
                WHERE f.id = %d
                  AND f.owner_id = %d
                  AND f.deleted_at IS NULL
                """.formatted(id, ownerId);

        return findOne(query);
    }

    public FileItemDto findById(Long id) {
        if (id == null) {
            return null;
        }

        String query = selectBase() + """
                WHERE f.id = %d
                  AND f.deleted_at IS NULL
                """.formatted(id);

        return findOne(query);
    }

    public FileItemDto save(FileItemDto fileItemDto) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            AgdrvFile saved = sql.sql().saveEntityWithCheck(FileItemConverter.dtoToEntity(fileItemDto));
            return saved == null || saved.getId() == null ? null : findByIdAndOwnerId(saved.getId(), fileItemDto.getOwnerId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long sumSizeByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return 0L;
        }

        String query = """
                SELECT COALESCE(SUM(f.file_size), 0)::BIGINT AS total_size
                FROM public.agdrv_files f
                WHERE f.owner_id = %d
                  AND f.deleted_at IS NULL
                """.formatted(ownerId);

        return fetchLong(query, "total_size");
    }

    public long sumSizeAll() {
        String query = """
                SELECT COALESCE(SUM(f.file_size), 0)::BIGINT AS total_size
                FROM public.agdrv_files f
                WHERE f.deleted_at IS NULL
                """;

        return fetchLong(query, "total_size");
    }

    public List<FileItemDto> findAllActiveImageFiles() {
        String query = selectBase() + """
                WHERE f.deleted_at IS NULL
                  AND (f.is_image = TRUE OR f.mime_type ILIKE 'image/%')
                ORDER BY f.id ASC
                """;

        return findMany(query);
    }

    public List<FileItemDto> findExpiredActiveFiles() {
        String query = selectBase() + """
                WHERE f.deleted_at IS NULL
                  AND f.expires_at IS NOT NULL
                  AND f.expires_at <= NOW()
                ORDER BY f.expires_at ASC, f.id ASC
                """;

        return findMany(query);
    }

    public boolean existsByOwnerIdAndFolderIdAndOriginalFilename(Long ownerId,
                                                                 Long folderId,
                                                                 String originalFilename,
                                                                 Long excludeId) {
        if (ownerId == null || originalFilename == null || originalFilename.isBlank()) {
            return false;
        }

        String escapedFilename = originalFilename.trim().replace("'", "''");
        String folderCondition = folderId == null ? "folder_id IS NULL" : "folder_id = %d".formatted(folderId);
        String excludeCondition = excludeId == null ? "" : " AND id <> %d".formatted(excludeId);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_files
                WHERE owner_id = %d
                  AND %s
                  AND deleted_at IS NULL
                  AND lower(original_filename) = lower('%s')
                  %s
                """.formatted(ownerId, folderCondition, escapedFilename, excludeCondition);

        return fetchCount(query) > 0;
    }

    public FileItemDto findByOwnerIdAndFolderIdAndOriginalFilename(Long ownerId,
                                                                   Long folderId,
                                                                   String originalFilename) {
        if (ownerId == null || originalFilename == null || originalFilename.isBlank()) {
            return null;
        }

        String escapedFilename = originalFilename.trim().replace("'", "''");
        String folderCondition = folderId == null ? "f.folder_id IS NULL" : "f.folder_id = %d".formatted(folderId);
        String query = selectBase() + """
                WHERE f.owner_id = %d
                  AND %s
                  AND f.deleted_at IS NULL
                  AND lower(f.original_filename) = lower('%s')
                ORDER BY f.id DESC
                LIMIT 1
                """.formatted(ownerId, folderCondition, escapedFilename);

        return findOne(query);
    }

    public long countActiveByStorageName(String storageName, Long excludeId) {
        if (storageName == null || storageName.isBlank()) {
            return 0L;
        }

        String escapedStorageName = storageName.trim().replace("'", "''");
        String excludeCondition = excludeId == null ? "" : " AND id <> %d".formatted(excludeId);
        String query = """
                SELECT COUNT(*) AS total
                FROM public.agdrv_files
                WHERE storage_filename = '%s'
                  AND deleted_at IS NULL
                  %s
                """.formatted(escapedStorageName, excludeCondition);

        return fetchCount(query);
    }

    private List<FileItemDto> findMany(String query) {
        List<FileItemDto> result = new ArrayList<>();
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(query))) != null) {
                result.add(FileItemConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private FileItemDto findOne(String query) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : FileItemConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private long fetchCount(String query) {
        return fetchLong(query, "total");
    }

    private long fetchLong(String query, String column) {
        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long value = row == null ? null : row.getLong(column);
            return value == null ? 0L : value;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String selectBase() {
        return """
                SELECT
                    f.id,
                    f.created_at,
                    f.updated_at,
                    f.deleted_at,
                    f.owner_id,
                    f.folder_id,
                    folder.name AS folder_name,
                    f.original_filename,
                    f.storage_filename AS storage_name,
                    f.mime_type,
                    f.extension,
                    f.file_size AS size_bytes,
                    f.checksum AS checksum_sha256,
                    f.description,
                    f.expires_at,
                    f.preview_status,
                    f.is_image,
                    f.is_video
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                """;
    }

    private String buildFileQueryCondition(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return " AND f.original_filename ILIKE '%" + escapeSqlLiteral(query.trim()) + "%'";
    }

    private String buildFileScopeCondition(Long currentFolderId, String currentFolderPath, String scope) {
        if ("all".equalsIgnoreCase(scope)) {
            return "1 = 1";
        }
        if ("tree".equalsIgnoreCase(scope)) {
            if (currentFolderId == null || currentFolderPath == null || currentFolderPath.isBlank()) {
                return "1 = 1";
            }
            return "(f.folder_id = %d OR folder.path_key LIKE '%s/%%')"
                    .formatted(currentFolderId, escapeSqlLiteral(currentFolderPath));
        }
        return currentFolderId == null
                ? "f.folder_id IS NULL"
                : "f.folder_id = " + currentFolderId;
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private String buildFileOrderBy(String sortMode) {
        if ("name_desc".equalsIgnoreCase(sortMode)) {
            return "lower(f.original_filename) DESC, f.id DESC";
        }
        if ("date_newest".equalsIgnoreCase(sortMode)) {
            return "f.created_at DESC, lower(f.original_filename) ASC, f.id DESC";
        }
        if ("date_oldest".equalsIgnoreCase(sortMode)) {
            return "f.created_at ASC, lower(f.original_filename) ASC, f.id ASC";
        }
        if ("size_desc".equalsIgnoreCase(sortMode)) {
            return "COALESCE(f.file_size, 0) DESC, lower(f.original_filename) ASC, f.id DESC";
        }
        if ("size_asc".equalsIgnoreCase(sortMode)) {
            return "COALESCE(f.file_size, 0) ASC, lower(f.original_filename) ASC, f.id ASC";
        }
        if ("type_asc".equalsIgnoreCase(sortMode)) {
            return "lower(COALESCE(f.extension, '')) ASC, lower(f.original_filename) ASC, f.id ASC";
        }
        return "lower(f.original_filename) ASC, f.id ASC";
    }
}
