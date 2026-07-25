package org.agty.drive;

import org.agty.drive.config.AppTime;
import org.agty.drive.support.IntegrationTestBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ApplicationTests extends IntegrationTestBootstrap {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldUseConfiguredTimeZoneForJvmAndDatabaseSession() throws Exception {
        assertEquals(AppTime.getZoneId(), TimeZone.getDefault().toZoneId());

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW TIME ZONE")) {
            resultSet.next();
            assertEquals(AppTime.getZoneIdValue(), resultSet.getString(1));
        }
    }

}
