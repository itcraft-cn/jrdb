package com.dbmmap.core.config;

import javax.sql.DataSource;

/**
 * Configuration for JDBC connection.
 */
public class JdbcConfig {

    private final DataSource dataSource;
    private final int maxPoolSize;
    private final int minPoolSize;
    private final int connectionTimeout;

    private JdbcConfig(Builder builder) {
        this.dataSource = builder.dataSource;
        this.maxPoolSize = builder.maxPoolSize;
        this.minPoolSize = builder.minPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        validate();
    }

    private void validate() {
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive");
        }
        if (minPoolSize < 0) {
            throw new IllegalArgumentException("minPoolSize must be non-negative");
        }
        if (connectionTimeout <= 0) {
            throw new IllegalArgumentException("connectionTimeout must be positive");
        }
        if (minPoolSize > maxPoolSize) {
            throw new IllegalArgumentException("minPoolSize must not exceed maxPoolSize");
        }
    }

    public DataSource getDataSource() { return dataSource; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getMinPoolSize() { return minPoolSize; }
    public int getConnectionTimeout() { return connectionTimeout; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private DataSource dataSource;
        private int maxPoolSize = 10;
        private int minPoolSize = 1;
        private int connectionTimeout = 30000;

        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder minPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        public Builder connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public JdbcConfig build() { return new JdbcConfig(this); }
    }
}