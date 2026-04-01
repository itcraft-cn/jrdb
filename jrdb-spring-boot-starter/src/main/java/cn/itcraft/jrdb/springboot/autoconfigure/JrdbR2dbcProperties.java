package cn.itcraft.jrdb.springboot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jrdb.r2dbc")
public class JrdbR2dbcProperties {

    private boolean enabled = true;
    private int initialPoolSize = 5;
    private int maxPoolSize = 10;
    private long maxLifeTimeMinutes = 30;
    private long maxIdleTimeMinutes = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getInitialPoolSize() {
        return initialPoolSize;
    }

    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getMaxLifeTimeMinutes() {
        return maxLifeTimeMinutes;
    }

    public void setMaxLifeTimeMinutes(long maxLifeTimeMinutes) {
        this.maxLifeTimeMinutes = maxLifeTimeMinutes;
    }

    public long getMaxIdleTimeMinutes() {
        return maxIdleTimeMinutes;
    }

    public void setMaxIdleTimeMinutes(long maxIdleTimeMinutes) {
        this.maxIdleTimeMinutes = maxIdleTimeMinutes;
    }
}