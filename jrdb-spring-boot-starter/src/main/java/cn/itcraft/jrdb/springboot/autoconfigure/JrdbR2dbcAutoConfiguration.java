package cn.itcraft.jrdb.springboot.autoconfigure;

import cn.itcraft.jrdb.r2dbc.mapping.R2dbcMapperRegistry;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSession;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSessionImpl;
import cn.itcraft.jrdb.springboot.R2dbcTemplate;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Configuration
@ConditionalOnClass({R2dbcSession.class, Flux.class})
@ConditionalOnProperty(prefix = "jrdb.r2dbc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JrdbR2dbcProperties.class)
public class JrdbR2dbcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(R2dbcMapperRegistry.class)
    public R2dbcMapperRegistry r2dbcMapperRegistry() {
        return new R2dbcMapperRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(R2dbcSession.class)
    public R2dbcSession r2dbcSession(ConnectionFactory connectionFactory, JrdbR2dbcProperties properties) {
        ConnectionPool pool;
        if (connectionFactory instanceof ConnectionPool) {
            pool = (ConnectionPool) connectionFactory;
        } else {
            ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
                .initialSize(properties.getInitialPoolSize())
                .maxSize(properties.getMaxPoolSize())
                .maxLifeTime(Duration.ofMinutes(properties.getMaxLifeTimeMinutes()))
                .maxIdleTime(Duration.ofMinutes(properties.getMaxIdleTimeMinutes()))
                .build();
            pool = new ConnectionPool(poolConfig);
        }
        return new R2dbcSessionImpl(pool);
    }

    @Bean
    @ConditionalOnMissingBean(R2dbcTemplate.class)
    public R2dbcTemplate r2dbcTemplate(R2dbcSession session, R2dbcMapperRegistry mapperRegistry) {
        return new R2dbcTemplate(session, mapperRegistry);
    }
}