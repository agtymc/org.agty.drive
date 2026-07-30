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
