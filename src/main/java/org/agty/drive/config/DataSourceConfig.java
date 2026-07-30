package org.agty.drive.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String baseUrl = "jdbc:postgresql://%s:%s/%s".formatted(
                LocalConfig.getString("db.agtydrive.server", "localhost"),
                LocalConfig.getString("db.agtydrive.port", "5432"),
                LocalConfig.getString("db.agtydrive.database", "agtydrive")
        );
        String schema = LocalConfig.getString("db.agtydrive.schema", "public");
        String url = AppTime.buildJdbcUrl(baseUrl, schema);

        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(url)
                .username(LocalConfig.getString("db.agtydrive.user", "postgres"))
                .password(LocalConfig.getString("db.agtydrive.password", ""))
                .driverClassName("org.postgresql.Driver")
                .build();
        dataSource.setConnectionInitSql(AppTime.getSessionTimeZoneSql());
        return dataSource;
    }
}
