package br.ufmg.mes.shop.support;

import java.util.Arrays;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public final class PostgresTestDatabase {

    private PostgresTestDatabase() {
    }

    public static PostgreSQLContainer<?> create(String databaseName) {
        PostgreSQLContainer<?> database = new PostgreSQLContainer<>(
                "postgres@sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74")
                .withUrlParam("currentSchema", "shop")
                .withDatabaseName(databaseName)
                .withUsername("shop_test")
                .withPassword("shop_test");
        for (String script : Arrays.asList("V001__foundation.sql", "V002__products.sql", "V003__users.sql",
                "V004__orders.sql")) {
            database.withCopyFileToContainer(MountableFile.forClasspathResource("db/schema/" + script),
                    "/docker-entrypoint-initdb.d/" + script);
        }
        return database;
    }

    public static void applyTo(PostgreSQLContainer<?> database, ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "spring.datasource.url=" + database.getJdbcUrl(),
                "spring.datasource.username=" + database.getUsername(),
                "spring.datasource.password=" + database.getPassword())
                .applyTo(context.getEnvironment());
    }
}
