package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Lifecycle proof for {@link PreUpgradeFixtureSchema}: creating it reaches the actual Flyway
 * V300000325 boundary with {@code note.deleted_at} still present and accepts a raw-JDBC fixture
 * row; closing it drops only the owned schema and leaves the enclosing suite schema (this test's
 * own database) completely untouched.
 */
@SpringBootTest
@ActiveProfiles("test")
class PreUpgradeFixtureSchemaTest {

  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void ownedSchemaReachesVersion325AndIsDroppedWithoutTouchingTheSuiteSchema() throws Exception {
    String ownerSchemaName;
    try (Connection connection = dataSource.getConnection()) {
      ownerSchemaName = connection.getCatalog();
    }

    Integer suiteDeletedAtColumnsBefore = countNoteDeletedAtColumns(ownerSchemaName);
    Long suiteHistoryRowsBefore = countFlywayHistoryRows(ownerSchemaName);

    String fixtureSchemaName;
    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      fixtureSchemaName = fixture.schemaName();
      assertThat(fixtureSchemaName, equalTo(ownerSchemaName + "_v325_fixture"));
      assertThat(schemaExists(fixtureSchemaName), is(true));

      Connection raw = fixture.connection();
      try (Statement statement = raw.createStatement();
          ResultSet resultSet =
              statement.executeQuery(
                  "SELECT version FROM flyway_schema_history "
                      + "WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1")) {
        assertThat(resultSet.next(), is(true));
        assertThat(resultSet.getString("version"), equalTo("300000325"));
      }

      try (Statement statement = raw.createStatement();
          ResultSet resultSet =
              statement.executeQuery("SHOW COLUMNS FROM note LIKE 'deleted_at'")) {
        assertThat(resultSet.next(), is(true));
      }

      // Accepts a raw-JDBC fixture row before the actual 326 conversion runs (a later slice).
      try (Statement statement = raw.createStatement()) {
        statement.execute(
            "INSERT INTO user (name, external_identifier) VALUES ('Pre-upgrade Fixture Owner',"
                + " 'pre-upgrade-fixture-owner')");
      }
      try (Statement statement = raw.createStatement();
          ResultSet resultSet =
              statement.executeQuery(
                  "SELECT COUNT(*) AS total FROM user"
                      + " WHERE external_identifier = 'pre-upgrade-fixture-owner'")) {
        assertThat(resultSet.next(), is(true));
        assertThat(resultSet.getInt("total"), equalTo(1));
      }
    }

    assertThat(schemaExists(fixtureSchemaName), is(false));
    assertThat(countNoteDeletedAtColumns(ownerSchemaName), equalTo(suiteDeletedAtColumnsBefore));
    assertThat(countFlywayHistoryRows(ownerSchemaName), equalTo(suiteHistoryRowsBefore));
  }

  private Integer countNoteDeletedAtColumns(String schemaName) {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = ? AND table_name = 'note' AND column_name = 'deleted_at'
        """,
        Integer.class,
        schemaName);
  }

  private Long countFlywayHistoryRows(String schemaName) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + schemaName + ".flyway_schema_history", Long.class);
  }

  private boolean schemaExists(String schemaName) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?",
            Integer.class,
            schemaName);
    return count != null && count > 0;
  }
}
