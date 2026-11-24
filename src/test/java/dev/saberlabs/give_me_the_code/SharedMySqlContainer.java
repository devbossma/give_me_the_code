package dev.saberlabs.give_me_the_code;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

/**
 * One MySQL container shared by every test class in the suite, started eagerly in a static
 * initializer rather than left to {@code @Testcontainers}' own lifecycle management: the latter
 * tears the container down between test classes that otherwise share one cached Spring context,
 * which then fails every test class after the first.
 */
public abstract class SharedMySqlContainer {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }
}
