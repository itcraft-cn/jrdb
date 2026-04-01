package cn.itcraft.jrdb.r2dbc.config;

public class R2dbcConfig {

    private boolean cacheEnabled = false;
    private R2dbcExecutorConfig executorConfig = new R2dbcExecutorConfig();

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public R2dbcExecutorConfig getExecutorConfig() {
        return executorConfig;
    }

    public R2dbcConfig executorConfig(R2dbcExecutorConfig executorConfig) {
        this.executorConfig = executorConfig;
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final R2dbcConfig config = new R2dbcConfig();

        public Builder cacheEnabled(boolean cacheEnabled) {
            config.setCacheEnabled(cacheEnabled);
            return this;
        }

        public Builder executorConfig(R2dbcExecutorConfig executorConfig) {
            config.executorConfig(executorConfig);
            return this;
        }

        public R2dbcConfig build() {
            return config;
        }
    }
}