package com.tarasantoniuk.common;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers.
 * <p>
 * Provides PostgreSQL 17 and Redis 7 containers for production-like testing.
 * Uses the singleton container pattern — containers stay alive for the entire
 * test suite, preventing stale connection issues when Spring caches contexts.
 * <p>
 * This class intentionally has NO Spring context annotations (@SpringBootTest,
 *
 * @DataJpaTest, etc.) so that subclasses can choose their own context type:
 * <ul>
 *   <li>Full context: add @SpringBootTest on the subclass</li>
 *   <li>JPA slice: add @DataJpaTest + @AutoConfigureTestDatabase(replace = NONE)</li>
 * </ul>
 */
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres;
    static final GenericContainer<?> redis;

    static {
        postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgres.start();

        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Schema management — 'create' (NOT 'create-drop') to avoid DROP attempts
        // after Testcontainers shuts down PostgreSQL, which causes HikariPool errors
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }
}
