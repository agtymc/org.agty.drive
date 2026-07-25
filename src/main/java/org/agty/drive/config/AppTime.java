package org.agty.drive.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.TimeZone;

public final class AppTime {

    public static final String DEFAULT_TIME_ZONE = "Europe/Moscow";

    private static final DateTimeFormatter DB_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<DateTimeFormatter> DB_INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DB_DATE_TIME
    );

    private AppTime() {
    }

    public static ZoneId getZoneId() {
        String configured = LocalConfig.getString("app.timezone", DEFAULT_TIME_ZONE);
        try {
            return ZoneId.of(configured);
        } catch (Exception ignored) {
            return ZoneId.of(DEFAULT_TIME_ZONE);
        }
    }

    public static String getZoneIdValue() {
        return getZoneId().getId();
    }

    public static void applyJvmDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(getZoneId()));
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(getZoneId());
    }

    public static LocalDate today() {
        return LocalDate.now(getZoneId());
    }

    public static String nowForDatabase() {
        return formatForDatabase(now());
    }

    public static String formatForDatabase(LocalDateTime value) {
        return value == null ? null : value.format(DB_DATE_TIME);
    }

    public static LocalDateTime parseDatabaseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DB_INPUT_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public static String formatForTitle(String value, String titlePattern) {
        LocalDateTime parsed = parseDatabaseDateTime(value);
        if (parsed == null) {
            return value == null || value.isBlank() ? "—" : value;
        }
        return parsed.format(DateTimeFormatter.ofPattern(titlePattern));
    }

    public static String buildJdbcUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        String separator = baseUrl.contains("?") ? "&" : "?";
        String option = URLEncoder.encode("-c TimeZone=" + getZoneIdValue(), StandardCharsets.UTF_8);
        return baseUrl + separator + "options=" + option;
    }

    public static String getSessionTimeZoneSql() {
        return "SET TIME ZONE '" + getZoneIdValue().replace("'", "''") + "'";
    }

    public static void applySessionTimeZone(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(getSessionTimeZoneSql());
        }
    }
}
