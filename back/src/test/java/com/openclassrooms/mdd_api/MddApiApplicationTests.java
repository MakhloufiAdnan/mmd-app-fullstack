package com.openclassrooms.mdd_api;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

/**
 * Tests unitaires du bootstrap Spring (main).
 */
class MddApiApplicationMainTest {

    @Test
    void main_shouldDelegateToSpringApplicationRun() {
        // Arrange
        String[] args = {"--any=arg"};

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {

            // Act
            MddApiApplication.main(args);

            // Assert
            mocked.verify(() -> SpringApplication.run(MddApiApplication.class, args));
        }
    }
}
