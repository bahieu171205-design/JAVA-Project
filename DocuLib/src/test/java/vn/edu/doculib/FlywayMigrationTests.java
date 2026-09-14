package vn.edu.doculib;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTests {

    @Test
    void migrationsBuildANewIsolatedDatabaseWithoutUsingPersonalMysql() throws Exception {
        String url = "jdbc:h2:mem:doculib_flyway;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        var result = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(result.migrationsExecuted).isEqualTo(2);
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            assertThat(count(statement, "resource_materials")).isZero();
            assertThat(count(statement, "user_accounts")).isZero();
            assertThat(count(statement, "code_sequences")).isEqualTo(2);
            assertThat(count(statement, "acquisition_status_history")).isZero();
            assertThat(count(statement, "material_audit_logs")).isZero();
        }
    }

    private int count(java.sql.Statement statement, String table) throws Exception {
        try (var resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
