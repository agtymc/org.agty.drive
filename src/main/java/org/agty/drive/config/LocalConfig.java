package org.agty.drive.config;

import org.agty.utils.AgtyUtils;

import java.util.Properties;

public final class LocalConfig {

    private static Properties properties;

    private LocalConfig() {
    }

    private static Properties getProperties() {
        if (properties == null) {
            properties = PropertyFactory.loadProperties("config.ini");
        }
        return properties;
    }

    public static Object get(String key) {
        String override = getOverride(key);
        return override != null ? override : getProperties().get(key);
    }

    public static Integer getInt(String key) {
        return Integer.parseInt(getProperties().getProperty(key));
    }

    public static String getString(String key) {
        String override = getOverride(key);
        return override != null ? override : getProperties().getProperty(key);
    }

    public static String getString(String key, String defaultValue) {
        String value = getString(key);
        return AgtyUtils.stringNonNullOrEmpty(value) ? value : defaultValue;
    }

    public static Long getLong(String key) {
        return Long.parseLong(getProperties().getProperty(key));
    }

    public static Boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    private static String getOverride(String key) {
        String systemValue = System.getProperty(key);
        if (AgtyUtils.stringNonNullOrEmpty(systemValue)) {
            return systemValue;
        }

        String envValue = System.getenv(toEnvKey(key));
        if (AgtyUtils.stringNonNullOrEmpty(envValue)) {
            return envValue;
        }

        return null;
    }

    private static String toEnvKey(String key) {
        return key == null ? "" : key.trim().replace('.', '_').toUpperCase();
    }
}
