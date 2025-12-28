package com.openclassrooms.mdd_api.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Classe de base pour les tests d'intégration avec MySQL via Testcontainers.
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
}
