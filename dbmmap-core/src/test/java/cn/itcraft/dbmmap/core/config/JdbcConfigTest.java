package cn.itcraft.dbmmap.core.config;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JdbcConfigTest {

    @Test
    void shouldCreateWithDefaults() {
        DataSource dataSource = mock(DataSource.class);
        
        JdbcConfig config = JdbcConfig.builder()
                .dataSource(dataSource)
                .build();

        assertNotNull(config);
        assertEquals(dataSource, config.getDataSource());
        assertEquals(10, config.getMaxPoolSize());
        assertEquals(1, config.getMinPoolSize());
        assertEquals(30000, config.getConnectionTimeout());
    }

    @Test
    void shouldValidateDataSource() {
        assertThrows(NullPointerException.class, () -> 
            JdbcConfig.builder().build());
    }

    @Test
    void shouldAcceptCustomValues() {
        DataSource dataSource = mock(DataSource.class);
        
        JdbcConfig config = JdbcConfig.builder()
                .dataSource(dataSource)
                .maxPoolSize(20)
                .minPoolSize(5)
                .connectionTimeout(60000)
                .build();

        assertEquals(20, config.getMaxPoolSize());
        assertEquals(5, config.getMinPoolSize());
        assertEquals(60000, config.getConnectionTimeout());
    }

    @Test
    void shouldThrowOnNullDataSource() {
        assertThrows(NullPointerException.class, () -> JdbcConfig.builder().build());
    }

    @Test
    void shouldValidateMaxPoolSize() {
        DataSource dataSource = mock(DataSource.class);
        assertThrows(IllegalArgumentException.class, () ->
            JdbcConfig.builder()
                .dataSource(dataSource)
                .maxPoolSize(0)
                .build());
    }

    @Test
    void shouldValidateMinPoolSize() {
        DataSource dataSource = mock(DataSource.class);
        assertThrows(IllegalArgumentException.class, () ->
            JdbcConfig.builder()
                .dataSource(dataSource)
                .minPoolSize(-1)
                .build());
    }

    @Test
    void shouldValidateConnectionTimeout() {
        DataSource dataSource = mock(DataSource.class);
        assertThrows(IllegalArgumentException.class, () ->
            JdbcConfig.builder()
                .dataSource(dataSource)
                .connectionTimeout(0)
                .build());
    }

    @Test
    void shouldValidateMinLessThanMax() {
        DataSource dataSource = mock(DataSource.class);
        assertThrows(IllegalArgumentException.class, () ->
            JdbcConfig.builder()
                .dataSource(dataSource)
                .minPoolSize(20)
                .maxPoolSize(10)
                .build());
    }
}