package org.agty.drive.config;

import org.agty.agtysql.config.AgtySqlConfig;
import org.agty.utils.MainArgs;

public final class DbConfig {

    private DbConfig() {
    }

    private static boolean getDebug() {
        return MainArgs.isSet("debug");
    }

    private static String requireString(String key) {
        String value = LocalConfig.getString(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config key: " + key);
        }
        return value;
    }

    private static int requireInt(String key) {
        String value = requireString(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid integer for config key: " + key + " = " + value);
        }
    }

    public static AgtySqlConfig getConfig() {
        return new AgtySqlConfig()
                .setServer(requireString("db.agtydrive.server"))
                .setPort(requireInt("db.agtydrive.port"))
                .setUser(requireString("db.agtydrive.user"))
                .setPassword(requireString("db.agtydrive.password"))
                .setDatabase(requireString("db.agtydrive.database"))
                .setSchema(requireString("db.agtydrive.schema"))
                .setDriver("pgsql")
                .setEncoding("UTF-8")
                .setDebug(getDebug());
    }
}
