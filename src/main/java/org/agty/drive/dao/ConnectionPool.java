package org.agty.drive.dao;

import org.agty.drive.config.DbConfig;
import org.agty.drive.config.LocalConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionPool {

    private static final int DEFAULT_MAX_POOL_SIZE = 32;
    private static final int DEFAULT_MAX_LIFETIME_MIN = 30;
    private static final long DEFAULT_BORROW_TIMEOUT_MS = 300L;
    private static final Map<String, AgtySQLPool> POOLS = new ConcurrentHashMap<>();

    public static final AgtySQLPool POOL = new AgtySQLPool(
            DbConfig.getConfig(),
            getConfiguredInt("db.pool.max.size", DEFAULT_MAX_POOL_SIZE, 1),
            Duration.ofMinutes(getConfiguredInt("db.pool.max.lifetime.min", DEFAULT_MAX_LIFETIME_MIN, 1)),
            Duration.ofMillis(getConfiguredLong("db.pool.borrow.timeout.ms", DEFAULT_BORROW_TIMEOUT_MS, 1L))
    );

    private ConnectionPool() {
    }

    private static int getConfiguredInt(String key, int defaultValue, int minValue) {
        try {
            int value = Integer.parseInt(LocalConfig.getString(key, Integer.toString(defaultValue)));
            return Math.max(minValue, value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static long getConfiguredLong(String key, long defaultValue, long minValue) {
        try {
            long value = Long.parseLong(LocalConfig.getString(key, Long.toString(defaultValue)));
            return Math.max(minValue, value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static AgtySQLPool createPool(String poolName) {
        String normalizedName = normalizedName(poolName);
        String keyPrefix = "db.pool." + normalizedName;
        return new AgtySQLPool(
                PoolDbConfigFactory.getConfig(normalizedName),
                getConfiguredInt(keyPrefix + ".max.size", DEFAULT_MAX_POOL_SIZE, 1),
                Duration.ofMinutes(getConfiguredInt(keyPrefix + ".max.lifetime.min", DEFAULT_MAX_LIFETIME_MIN, 1)),
                Duration.ofMillis(getConfiguredLong(keyPrefix + ".borrow.timeout.ms", DEFAULT_BORROW_TIMEOUT_MS, 1L))
        );
    }

    public static AgtySQLPool get(String name) {
        String key = normalizedName(name);
        return POOLS.computeIfAbsent(key, ConnectionPool::createPool);
    }

    private static String normalizedName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Connection pool name must not be empty");
        }
        return trimmed;
    }
}
