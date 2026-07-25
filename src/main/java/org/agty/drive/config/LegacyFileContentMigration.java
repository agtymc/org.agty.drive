package org.agty.drive.config;

import org.agty.drive.services.StoragePathSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class LegacyFileContentMigration {

    private static final Logger log = LoggerFactory.getLogger(LegacyFileContentMigration.class);

    private LegacyFileContentMigration() {
    }

    public static void migrateIfNeeded() {
        String baseUrl = "jdbc:postgresql://%s:%s/%s".formatted(
                LocalConfig.getString("db.agtydrive.server", "localhost"),
                LocalConfig.getString("db.agtydrive.port", "5432"),
                LocalConfig.getString("db.agtydrive.database", "agtydrive")
        );
        String url = AppTime.buildJdbcUrl(baseUrl);

        Path rootPath = StoragePathSupport.resolveRootPath(LocalConfig.getString("storage.content_dir", "content"));
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory for legacy migration: " + rootPath, e);
        }

        try (Connection connection = DriverManager.getConnection(
                url,
                LocalConfig.getString("db.agtydrive.user", "postgres"),
                LocalConfig.getString("db.agtydrive.password", "")
        )) {
            AppTime.applySessionTimeZone(connection);
            if (!legacyTableExists(connection)) {
                return;
            }

            List<Long> largeObjectIds = new ArrayList<>();
            int migratedCount = 0;

            String selectSql = """
                    SELECT
                        fc.file_id,
                        f.storage_filename,
                        f.extension,
                        f.checksum,
                        f.created_at,
                        fc.content_oid,
                        CASE
                            WHEN fc.content_bytea IS NOT NULL THEN fc.content_bytea
                            WHEN fc.content_oid IS NOT NULL THEN lo_get(fc.content_oid)
                            ELSE NULL
                        END AS content_data
                    FROM public.agdrv_file_content fc
                    JOIN public.agdrv_files f ON f.id = fc.file_id
                    ORDER BY fc.file_id
                    """;

            try (PreparedStatement statement = connection.prepareStatement(selectSql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Long fileId = resultSet.getLong("file_id");
                    String storageName = resultSet.getString("storage_filename");
                    String extension = resultSet.getString("extension");
                    String checksum = resultSet.getString("checksum");
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    Long contentOid = resultSet.getObject("content_oid") == null ? null : resultSet.getLong("content_oid");
                    byte[] content = resultSet.getBytes("content_data");

                    if (content == null) {
                        throw new IllegalStateException("Legacy file content is empty for file_id=" + fileId);
                    }

                    if (storageName == null || storageName.isBlank()) {
                        storageName = StoragePathSupport.buildStorageName(
                                checksum,
                                extension,
                                createdAt == null ? AppTime.today() : createdAt.toLocalDateTime().toLocalDate()
                        );
                        updateStorageName(connection, fileId, storageName);
                    }

                    Path targetPath = StoragePathSupport.resolveContentPath(rootPath, storageName);
                    Path parent = targetPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.write(targetPath, content);
                    migratedCount++;

                    if (contentOid != null && contentOid > 0) {
                        largeObjectIds.add(contentOid);
                    }
                }
            }

            unlinkLargeObjects(connection, largeObjectIds);
            log.info("Legacy file content migration completed. Migrated files: {}", migratedCount);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to migrate legacy file content", e);
        }
    }

    private static boolean legacyTableExists(Connection connection) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = 'agdrv_file_content'
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    private static void updateStorageName(Connection connection, Long fileId, String storageName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE public.agdrv_files SET storage_filename = ? WHERE id = ?"
        )) {
            statement.setString(1, storageName);
            statement.setLong(2, fileId);
            statement.executeUpdate();
        }
    }

    private static void unlinkLargeObjects(Connection connection, List<Long> objectIds) throws SQLException {
        if (objectIds.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("SELECT lo_unlink(?)")) {
            for (Long objectId : objectIds) {
                statement.setLong(1, objectId);
                statement.execute();
            }
        }
    }
}
