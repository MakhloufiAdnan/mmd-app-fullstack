package com.openclassrooms.mdd_api.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

/**
 * Classe de base pour les tests d'intégration avec MySQL via Testcontainers.
 *
 * Inclut un nettoyage DB "hard" avant chaque test pour éviter les conflits de FK
 * entre classes de tests, car le container MySQL est partagé (static).
 */
@Testcontainers
public abstract class AbstractMySqlIntegrationTest {

    @SuppressWarnings("resource")
    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("mdd")
                    .withUsername("test")
                    .withPassword("test");

    static {
        MYSQL.start();
        Runtime.getRuntime().addShutdownHook(new Thread(MYSQL::stop));
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");

        registry.add("oc.app.jwtSecret", () -> "test-test-test-test-test-test-test-test");
        registry.add("oc.app.jwtExpirationMs", () -> 60_000);
        registry.add("oc.app.refreshTokenExpirationMs", () -> 600_000);
        registry.add("oc.app.cookieSecure", () -> false);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Nettoyage DB avant chaque test :
     * - désactive les FK
     * - TRUNCATE toutes les tables du schema courant
     * - réactive les FK
     */
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                """,
                String.class
        );

        for (String table : tables) {
            if ("flyway_schema_history".equalsIgnoreCase(table)) continue;
            jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
