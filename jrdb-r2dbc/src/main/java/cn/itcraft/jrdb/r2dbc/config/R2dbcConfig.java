package cn.itcraft.jrdb.r2dbc.config;

public class R2dbcConfig {

    private boolean cacheEnabled = false;

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
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

        public R2dbcConfig build() {
            return config;
        }
    }
}