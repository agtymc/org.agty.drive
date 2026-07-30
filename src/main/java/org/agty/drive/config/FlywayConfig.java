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

package org.agty.drive.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        String schema = LocalConfig.getString("db.agtydrive.schema", "public");
        String baseUrl = "jdbc:postgresql://%s:%s/%s".formatted(
                LocalConfig.getString("db.agtydrive.server", "localhost"),
                LocalConfig.getString("db.agtydrive.port", "5432"),
                LocalConfig.getString("db.agtydrive.database", "agtydrive")
        );
        String url = AppTime.buildJdbcUrl(baseUrl, schema);

        return Flyway.configure()
                .dataSource(
                        url,
                        LocalConfig.getString("db.agtydrive.user", "postgres"),
                        LocalConfig.getString("db.agtydrive.password", "")
                )
                .schemas(schema)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
