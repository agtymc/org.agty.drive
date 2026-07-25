package org.agty.drive.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        String baseUrl = "jdbc:postgresql://%s:%s/%s".formatted(
                LocalConfig.getString("db.agtydrive.server", "localhost"),
                LocalConfig.getString("db.agtydrive.port", "5432"),
                LocalConfig.getString("db.agtydrive.database", "agtydrive")
        );
        String url = AppTime.buildJdbcUrl(baseUrl);

        return Flyway.configure()
                .dataSource(
                        url,
                        LocalConfig.getString("db.agtydrive.user", "postgres"),
                        LocalConfig.getString("db.agtydrive.password", "")
                )
                .schemas(LocalConfig.getString("db.agtydrive.schema", "public"))
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
