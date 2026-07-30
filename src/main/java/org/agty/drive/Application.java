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

package org.agty.drive;

import org.agty.drive.config.AppTime;
import org.agty.drive.config.ConfigBootstrap;
import org.agty.drive.config.LegacyFileContentMigration;
import org.agty.drive.dao.ConnectionPool;
import org.agty.utils.MainArgs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
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
