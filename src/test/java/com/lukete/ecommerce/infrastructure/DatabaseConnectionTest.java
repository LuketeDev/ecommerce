package com.lukete.ecommerce.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectionTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToPostgres() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldUsePostgresDatabase() {
        String database = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class);

        assertThat(database).isEqualTo("ecommerce_test");
    }
}