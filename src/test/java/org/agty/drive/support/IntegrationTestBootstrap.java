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

package org.agty.drive.support;

import org.agty.drive.config.AppTime;
import org.agty.drive.config.PropertyFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

public abstract class IntegrationTestBootstrap {

    private static final TestDatabaseEnvironment TEST_DATABASE_ENVIRONMENT = TestDatabaseEnvironment.start();

    protected static String getTestDatabaseName() {
        return TEST_DATABASE_ENVIRONMENT.databaseName;
    }

    private static final class TestDatabaseEnvironment {

        private final String databaseName;
        private final Path contentDir;
        private final String adminUrl;
        private final String username;
        private final String password;

        private TestDatabaseEnvironment(String databaseName,
                                        Path contentDir,
                                        String adminUrl,
                                        String username,
                                        String password) {
            this.databaseName = databaseName;
            this.contentDir = contentDir;
            this.adminUrl = adminUrl;
            this.username = username;
            this.password = password;
        }

        private static TestDatabaseEnvironment start() {
            Properties properties = PropertyFactory.loadProperties("config.ini");
            String server = properties.getProperty("db.agtydrive.server", "localhost");
            String port = properties.getProperty("db.agtydrive.port", "5432");
            String username = properties.getProperty("db.agtydrive.user", "postgres");
            String password = properties.getProperty("db.agtydrive.password", "");
            String databaseName = buildDatabaseName();
            Path contentDir = Path.of("target", "test-content", databaseName).toAbsolutePath().normalize();
            String adminUrl = "jdbc:postgresql://%s:%s/postgres".formatted(server, port);

            createDatabase(adminUrl, username, password, databaseName);
            applyOverrides(databaseName, contentDir);

            TestDatabaseEnvironment environment =
                    new TestDatabaseEnvironment(databaseName, contentDir, adminUrl, username, password);
            Runtime.getRuntime().addShutdownHook(new Thread(environment::cleanup, "agty-drive-test-db-cleanup"));
            return environment;
        }

        private static void applyOverrides(String databaseName, Path contentDir) {
            System.setProperty("db.agtydrive.database", databaseName);
            System.setProperty("db.agtydrive.schema", "public");
            System.setProperty("app.timezone", AppTime.DEFAULT_TIME_ZONE);
            System.setProperty("storage.content_dir", contentDir.toString());
        }

        private static void createDatabase(String adminUrl, String username, String password, String databaseName) {
            try (Connection connection = DriverManager.getConnection(adminUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE " + databaseName);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create test database: " + databaseName, e);
            }
        }

        private static String buildDatabaseName() {
            String suffix = Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "");
            String candidate = "agtydrive_test_" + suffix.toLowerCase();
            return candidate.substring(0, Math.min(candidate.length(), 63));
        }

        private void cleanup() {
            deleteContentDir();
            dropDatabase();
        }

        private void deleteContentDir() {
            if (!Files.exists(contentDir)) {
                return;
            }

            try (var paths = Files.walk(contentDir)) {
                paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }

        private void dropDatabase() {
            try (Connection connection = DriverManager.getConnection(adminUrl, username, password)) {
                terminateSessions(connection);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("DROP DATABASE IF EXISTS " + databaseName);
                }
            } catch (SQLException ignored) {
            }
        }

        private void terminateSessions(Connection connection) throws SQLException {
            String sql = """
                    SELECT pg_terminate_backend(pid)
                    FROM pg_stat_activity
                    WHERE datname = ?
                      AND pid <> pg_backend_pid()
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, databaseName);
                statement.execute();
            }
        }
    }
}
