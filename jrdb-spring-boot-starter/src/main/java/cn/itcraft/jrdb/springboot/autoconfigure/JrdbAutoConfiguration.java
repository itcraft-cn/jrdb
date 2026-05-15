package cn.itcraft.jrdb.springboot.autoconfigure;

import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.session.SessionFactory;
import cn.itcraft.jrdb.jdbc.spring.JrdbSpringTransactionTemplate;
import cn.itcraft.jrdb.jdbc.spring.JrdbTransactionTemplate;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import cn.itcraft.jrdb.springboot.JrdbTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass({SessionFactory.class, MemoryPool.class})
@ConditionalOnProperty(prefix = "jrdb", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JrdbProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JrdbAutoConfiguration {

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
    public MemoryPool memoryPool(JrdbProperties properties) {
        MemoryPoolConfig config = MemoryPoolConfig.builder()
            .preAllocMemSize(properties.getMemoryPool().getPreAllocMemSize())
            .queryCacheSize(properties.getMemoryPool().getQueryCacheSize())
            .build();
        return new MemoryPool(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public JrdbConfig dbMmapConfig(JrdbProperties properties) {
        return JrdbConfig.builder()
            .overflowStrategy(properties.getOverflowStrategy())
            .cacheLevel(properties.getCache().getLevel())
            .statementCacheSize(properties.getCache().getStatementCacheSize())
            .resultCacheSize(properties.getCache().getResultCacheSize())
            .resultCacheExpireSeconds(properties.getCache().getResultCacheExpireSeconds())
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionFactory sessionFactory(MemoryPool memoryPool, JrdbConfig config, MapperRegistry mapperRegistry,
                                         ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is required for jrdb");
        }
        return new SessionFactory(memoryPool, config, dataSource, mapperRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public JrdbTemplate jrdbTemplate(SessionFactory sessionFactory, MapperRegistry mapperRegistry) {
        return new JrdbTemplate(sessionFactory, mapperRegistry);
    }

    @Bean
    @ConditionalOnBean(SessionFactory.class)
    @ConditionalOnMissingBean(JrdbTransactionTemplate.class)
    public JrdbTransactionTemplate jrdbTransactionTemplate(SessionFactory sessionFactory) {
        return new JrdbTransactionTemplate(sessionFactory);
    }

    @Bean
    @ConditionalOnBean({SessionFactory.class, PlatformTransactionManager.class})
    @ConditionalOnMissingBean(JrdbSpringTransactionTemplate.class)
    public JrdbSpringTransactionTemplate jrdbSpringTransactionTemplate(
            SessionFactory sessionFactory,
            PlatformTransactionManager transactionManager) {
        return new JrdbSpringTransactionTemplate(sessionFactory, transactionManager);
    }
}