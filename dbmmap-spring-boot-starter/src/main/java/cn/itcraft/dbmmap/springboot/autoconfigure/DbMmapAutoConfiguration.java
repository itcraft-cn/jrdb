package cn.itcraft.dbmmap.springboot.autoconfigure;

import cn.itcraft.dbmmap.core.config.DbMmapConfig;
import cn.itcraft.dbmmap.core.mapping.MapperRegistry;
import cn.itcraft.dbmmap.core.memory.MemoryPool;
import cn.itcraft.dbmmap.core.memory.MemoryPoolConfig;
import cn.itcraft.dbmmap.core.session.Session;
import cn.itcraft.dbmmap.core.session.SessionImpl;
import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
import cn.itcraft.dbmmap.springboot.DbMmapTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass({Session.class, MemoryPool.class})
@ConditionalOnProperty(prefix = "dbmmap", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DbMmapProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class DbMmapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TypeHandlerRegistry typeHandlerRegistry() {
        return new TypeHandlerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public MapperRegistry mapperRegistry(TypeHandlerRegistry typeHandlerRegistry) {
        return new MapperRegistry(typeHandlerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public MemoryPool memoryPool(DbMmapProperties properties) {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
            .preAllocMemSize(properties.getMemoryPool().getPreAllocMemSize())
            .queryCacheSize(properties.getMemoryPool().getQueryCacheSize())
            .build();
        return new MemoryPool(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public DbMmapConfig dbMmapConfig(DbMmapProperties properties) {
        return DbMmapConfig.builder()
            .cacheLevel(properties.getCache().getLevel())
            .statementCacheSize(properties.getCache().getStatementCacheSize())
            .resultCacheSize(properties.getCache().getResultCacheSize())
            .resultCacheExpireSeconds(properties.getCache().getResultCacheExpireSeconds())
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Session session(MemoryPool memoryPool, DbMmapConfig config, ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is required for dbmmap");
        }
        return new SessionImpl(memoryPool, config, dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public DbMmapTemplate dbMmapTemplate(Session session, MapperRegistry mapperRegistry, 
                                         ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        return new DbMmapTemplate(session, mapperRegistry, dataSource);
    }
}