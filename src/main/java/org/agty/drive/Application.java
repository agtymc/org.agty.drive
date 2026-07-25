package org.agty.drive;

import org.agty.drive.config.AppTime;
import org.agty.drive.config.ConfigBootstrap;
import org.agty.drive.config.LegacyFileContentMigration;
import org.agty.drive.dao.ConnectionPool;
import org.agty.utils.MainArgs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        if (args.length != 0) {
            MainArgs.parse(args);
        }

        AppTime.applyJvmDefaultTimeZone();

        Path workingConfig = Paths.get(System.getProperty("user.dir"), "config.ini");
        if (!Files.exists(workingConfig) || !Files.isRegularFile(workingConfig)) {
            throw new IllegalStateException(
                    "config.ini not found in working directory: " + workingConfig.toAbsolutePath()
            );
        }

        ConfigBootstrap.applySystemProperties();
        LegacyFileContentMigration.migrateIfNeeded();
        ConnectionPool.POOL.preload(1);
        SpringApplication.run(Application.class, args);
    }

}
