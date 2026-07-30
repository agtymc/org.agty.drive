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

package org.agty.drive.dao;

import org.agty.agtysql.AgtySQL;
import org.agty.agtysql.config.AgtySqlConfig;
import org.agty.drive.config.AppTime;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AgtySQLPool implements AutoCloseable {

    private static final int CPU = Runtime.getRuntime().availableProcessors();
    private static final int RING_SIZE = 256;

    private final Ring[] rings = new Ring[CPU];
    private final ThreadLocal<Ring> threadRing;

    private final AgtySqlConfig config;
    private final int maxPoolSize;
    private final Duration maxLifetime;
    private final Duration defaultBorrowTimeout;

    private final AtomicInteger totalConnections = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object borrowMonitor = new Object();

    private final ScheduledExecutorService housekeeper;

    public AgtySQLPool(
            AgtySqlConfig config,
            int maxPoolSize,
            Duration maxLifetime,
            Duration defaultBorrowTimeout
    ) {
        this.config = config;
        this.maxPoolSize = maxPoolSize;
        this.maxLifetime = maxLifetime;
        this.defaultBorrowTimeout = defaultBorrowTimeout;

        for (int i = 0; i < rings.length; i++) {
            rings[i] = new Ring();
        }

        threadRing = ThreadLocal.withInitial(() -> {
            int id = (int) (Thread.currentThread().threadId() % CPU);
            return rings[id];
        });

        housekeeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AgtySQLPool-Housekeeper");
            t.setDaemon(true);
            return t;
        });

        housekeeper.scheduleAtFixedRate(this::cleanExtended, 30, 30, TimeUnit.SECONDS);
    }

    public AgtySQLPool(AgtySqlConfig config, int maxPoolSize, Duration maxLifetime) {
        this(config, maxPoolSize, maxLifetime, Duration.ofMillis(300));
    }

    public AgtySQLPool(AgtySqlConfig config, int maxPoolSize) {
        this(config, maxPoolSize, 30);
    }

    public AgtySQLPool(AgtySqlConfig config, int maxPoolSize, int durationMinutes) {
        this(config, maxPoolSize, Duration.ofMinutes(durationMinutes));
    }

    public PooledAgtySQL borrow() throws SQLException {
        return borrow(defaultBorrowTimeout);
    }

    public PooledAgtySQL borrow(Duration timeout) throws SQLException {
        if (closed.get()) {
            throw new IllegalStateException("Pool closed");
        }

        Duration effectiveTimeout = (timeout == null || timeout.isZero() || timeout.isNegative())
                ? Duration.ofMillis(1)
                : timeout;

        long deadline = System.nanoTime() + effectiveTimeout.toNanos();

        while (true) {
            PooledAgtySQL conn = tryBorrowOnce();
            if (conn != null) {
                return conn;
            }

            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new SQLException("Connection timeout");
            }

            synchronized (borrowMonitor) {
                if (closed.get()) {
                    throw new IllegalStateException("Pool closed");
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(borrowMonitor, remainingNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for a DB connection", e);
                }
            }
        }
    }

    private PooledAgtySQL tryBorrowOnce() {
        Ring ring = threadRing.get();
        PooledAgtySQL conn = ring.poll();
        if (conn != null && conn.tryBorrowFast()) {
            return conn;
        }

        while (true) {
            int cur = totalConnections.get();
            if (cur >= maxPoolSize) {
                break;
            }
            if (totalConnections.compareAndSet(cur, cur + 1)) {
                try {
                    return create();
                } catch (RuntimeException e) {
                    totalConnections.decrementAndGet();
                    throw e;
                }
            }
        }

        for (Ring currentRing : rings) {
            conn = currentRing.poll();
            if (conn != null && conn.tryBorrowFast()) {
                return conn;
            }
        }

        return null;
    }

    public PooledAgtySQL borrowExtended(Duration timeout) throws SQLException {
        Duration effectiveTimeout = (timeout == null || timeout.isZero() || timeout.isNegative())
                ? defaultBorrowTimeout
                : timeout;

        long deadline = System.nanoTime() + effectiveTimeout.toNanos();

        while (true) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new SQLException("Extended timeout");
            }

            PooledAgtySQL connection = borrow(Duration.ofNanos(remainingNanos));

            if (connection.isHealthyExtended()) {
                return connection;
            }

            connection.destroy();
        }
    }

    void release(PooledAgtySQL conn) {
        if (conn.destroyed) {
            return;
        }

        conn.borrowed = false;

        boolean offered = threadRing.get().offer(conn);
        if (!offered) {
            conn.destroy();
            return;
        }

        synchronized (borrowMonitor) {
            borrowMonitor.notify();
        }
    }

    private PooledAgtySQL create() {
        AgtySQL sql = new AgtySQL(config);
        try {
            AppTime.applySessionTimeZone(sql.getConnector().getConnection());
        } catch (SQLException e) {
            try {
                sql.close();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("Failed to apply database session timezone", e);
        }
        return new PooledAgtySQL(sql, this);
    }

    private void cleanExtended() {
        Instant now = Instant.now();

        for (Ring ring : rings) {
            synchronized (ring) {
                for (int i = 0; i < RING_SIZE; i++) {
                    PooledAgtySQL connection = ring.buffer[i];
                    if (connection == null) {
                        continue;
                    }
                    if (Duration.between(connection.createdAt, now).compareTo(maxLifetime) > 0) {
                        connection.destroy();
                        ring.buffer[i] = null;
                    }
                }
            }
        }
    }

    public void preload(int count) {
        for (int i = 0; i < count; i++) {
            if (totalConnections.get() >= maxPoolSize) {
                return;
            }

            totalConnections.incrementAndGet();

            try {
                PooledAgtySQL pooled = create();
                boolean offered = threadRing.get().offer(pooled);
                if (!offered) {
                    pooled.destroy();
                }
            } catch (RuntimeException e) {
                totalConnections.decrementAndGet();
                throw e;
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        synchronized (borrowMonitor) {
            borrowMonitor.notifyAll();
        }

        housekeeper.shutdownNow();

        for (Ring ring : rings) {
            for (int i = 0; i < RING_SIZE; i++) {
                PooledAgtySQL connection = ring.buffer[i];
                if (connection != null) {
                    connection.destroy();
                }
            }
        }
    }

    private static final class Ring {
        final PooledAgtySQL[] buffer = new PooledAgtySQL[RING_SIZE];
        int head;
        int tail;

        boolean offer(PooledAgtySQL connection) {
            synchronized (this) {
                int next = (tail + 1) & (RING_SIZE - 1);
                if (next == head) {
                    return false;
                }
                buffer[tail] = connection;
                tail = next;
                return true;
            }
        }

        PooledAgtySQL poll() {
            synchronized (this) {
                if (head == tail) {
                    return null;
                }

                PooledAgtySQL connection = buffer[head];
                buffer[head] = null;
                head = (head + 1) & (RING_SIZE - 1);
                return connection;
            }
        }
    }

    public static final class PooledAgtySQL implements AutoCloseable {

        final AgtySQL delegate;
        final AgtySQLPool pool;
        final Instant createdAt = Instant.now();

        volatile boolean borrowed;
        volatile boolean destroyed;

        PooledAgtySQL(AgtySQL delegate, AgtySQLPool pool) {
            this.delegate = delegate;
            this.pool = pool;
        }

        boolean tryBorrowFast() {
            if (destroyed || borrowed) {
                return false;
            }
            borrowed = true;
            return true;
        }

        boolean isHealthyExtended() {
            try {
                return !delegate.getConnector().getConnection().isClosed();
            } catch (Exception e) {
                return false;
            }
        }

        public AgtySQL sql() {
            return delegate;
        }

        @Override
        public void close() {
            if (destroyed) {
                return;
            }

            try {
                if (!delegate.getConnector().getConnection().getAutoCommit()) {
                    delegate.getConnector().getConnection().rollback();
                }
            } catch (Exception ignored) {
            }

            delegate.clearErrors();
            pool.release(this);
        }

        void destroy() {
            if (destroyed) {
                return;
            }

            destroyed = true;
            pool.totalConnections.decrementAndGet();

            synchronized (pool.borrowMonitor) {
                pool.borrowMonitor.notifyAll();
            }

            try {
                delegate.close();
            } catch (Exception ignored) {
            }
        }
    }
}
