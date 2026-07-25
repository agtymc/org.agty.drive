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

        String query = """
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
                    f.preview_status,
                    f.is_image,
                    f.is_video
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                WHERE f.owner_id = %d
                  AND f.deleted_at IS NULL
                ORDER BY f.created_at DESC, f.id DESC
                """.formatted(ownerId);

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

    public List<FileItemDto> findByOwnerIdAndFolderId(Long ownerId, Long folderId) {
        List<FileItemDto> result = new ArrayList<>();
        if (ownerId == null) {
            return result;
        }

        String folderCondition = folderId == null ? "f.folder_id IS NULL" : "f.folder_id = %d".formatted(folderId);
        String query = """
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
                    f.preview_status,
                    f.is_image,
                    f.is_video
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                WHERE f.owner_id = %d
                  AND %s
                  AND f.deleted_at IS NULL
                ORDER BY f.created_at DESC, f.id DESC
                """.formatted(ownerId, folderCondition);

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

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(sqlQuery);
            Long total = row == null ? null : row.getLong("total");
            return total == null ? 0L : total;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(sqlQuery);
            Long total = row == null ? null : row.getLong("total");
            return total == null ? 0L : total;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FileItemDto> searchByOwnerId(Long ownerId,
                                             String query,
                                             Long currentFolderId,
                                             String currentFolderPath,
                                             String scope,
                                             String sortMode,
                                             int offset,
                                             int limit) {
        List<FileItemDto> result = new ArrayList<>();
        if (ownerId == null || limit <= 0) {
            return result;
        }

        String queryCondition = buildFileQueryCondition(query);
        String scopeCondition = buildFileScopeCondition(currentFolderId, currentFolderPath, scope);
        String orderBy = buildFileOrderBy(sortMode);
        String sqlQuery = """
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
                    f.preview_status,
                    f.is_image,
                    f.is_video
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                WHERE f.owner_id = %d
                  AND f.deleted_at IS NULL
                  AND %s
                  %s
                ORDER BY %s
                OFFSET %d
                LIMIT %d
                """.formatted(ownerId, scopeCondition, queryCondition, orderBy, Math.max(0, offset), limit);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row;
            while ((row = sql.sql().list(Arguments.builder().setQuery(sqlQuery))) != null) {
                result.add(FileItemConverter.rowToDto(row));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public FileItemDto findByIdAndOwnerId(Long id, Long ownerId) {
        if (id == null || ownerId == null) {
            return null;
        }

        String query = """
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
                    f.preview_status,
                    f.is_image,
                    f.is_video
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                WHERE f.id = %d
                  AND f.owner_id = %d
                  AND f.deleted_at IS NULL
                """.formatted(id, ownerId);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : FileItemConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public FileItemDto findById(Long id) {
        if (id == null) {
            return null;
        }

        String query = """
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
                    f.preview_status,
                    f.is_image,
                    f.is_video
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                WHERE f.id = %d
                  AND f.deleted_at IS NULL
                """.formatted(id);

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            return row == null ? null : FileItemConverter.rowToDto(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long value = row == null ? null : row.getLong("total_size");
            return value == null ? 0L : value;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long sumSizeAll() {
        String query = """
                SELECT COALESCE(SUM(f.file_size), 0)::BIGINT AS total_size
                FROM public.agdrv_files f
                WHERE f.deleted_at IS NULL
                """;

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long value = row == null ? null : row.getLong("total_size");
            return value == null ? 0L : value;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FileItemDto> findAllActiveImageFiles() {
        List<FileItemDto> result = new ArrayList<>();
        String query = """
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
                    f.preview_status,
                    f.is_image,
                    f.is_video
                FROM public.agdrv_files f
                LEFT JOIN public.agdrv_folders folder ON folder.id = f.folder_id
                WHERE f.deleted_at IS NULL
                  AND (f.is_image = TRUE OR f.mime_type ILIKE 'image/%')
                ORDER BY f.id ASC
                """;

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

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total != null && total > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

        try (AgtySQLPool.PooledAgtySQL sql = ConnectionPool.POOL.borrow()) {
            SqlRow row = sql.sql().fetch(query);
            Long total = row == null ? null : row.getLong("total");
            return total == null ? 0L : total;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
