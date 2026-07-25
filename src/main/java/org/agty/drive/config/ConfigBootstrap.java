package org.agty.drive.config;

import java.util.Properties;

public final class ConfigBootstrap {

    private ConfigBootstrap() {
    }

    public static void applySystemProperties() {
        Properties properties = PropertyFactory.loadProperties("config.ini");
        for (String key : properties.stringPropertyNames()) {
            if (System.getProperty(key) == null) {
                System.setProperty(key, properties.getProperty(key));
            }
        }
    }
}
