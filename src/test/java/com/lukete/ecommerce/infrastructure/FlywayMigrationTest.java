package com.lukete.ecommerce.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest extends PostgresIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllMigrationsSuccessfully() {
        assertThat(flyway.info().applied())
                .isNotEmpty();

        assertThat(flyway.validateWithResult().validationSuccessful)
                .isTrue();
    }

    @Test
    void shouldCreateCategoriesTable() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'categories'
                """, Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldCreateProductsTable() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'products'
                """, Integer.class);

        assertThat(count).isEqualTo(1);
    }
}