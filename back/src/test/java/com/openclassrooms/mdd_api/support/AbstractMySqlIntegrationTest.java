package com.openclassrooms.mdd_api.support;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Classe de base pour les tests d'intégration avec MySQL via Testcontainers.
 */
@Testcontainers
public abstract class AbstractMySqlIntegrationTest {

    /**
     * Container MySQL partagé (1 instance pour toute la JVM de tests).
     */
    @SuppressWarnings("resource")
    @Container
    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("mdd")
                    .withUsername("test")
                    .withPassword("test");

    /**
     * Fournit à Spring les propriétés datasource et les propriétés applicatives minimales
     * en fonction du container MySQL.
     */
    @SuppressWarnings("unused")
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // S’assurer que le container est démarré avant d’exposer l’URL JDBC
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }

        // Datasource
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        // DB propre & déterministe pour les IT
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");

        // Propriétés minimales pour démarrer le contexte
        registry.add("oc.app.jwtSecret", () -> "test-test-test-test-test-test-test-test");
        registry.add("oc.app.jwtExpirationMs", () -> 60_000);
        registry.add("oc.app.refreshTokenExpirationMs", () -> 600_000);
        registry.add("oc.app.cookieSecure", () -> false);
    }

    /**
     * Stoppe le container.
     */
    @AfterAll
    static void stopContainer() {
        if (MYSQL != null && MYSQL.isRunning()) {
            MYSQL.stop();
        }
    }
}
