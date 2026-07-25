package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * Guards the placeholder-gated timezone-repair migration (see
 * db/migration/V300000233__repair_tz_skewed_quiz_answer_created_at.sql): with the no-op default
 * (`tz_repair=1=0`, matching every non-prod profile), the UPDATE must never touch a row, even one
 * whose created_at falls inside the repaired date band, because its id falls outside the literal
 * prod-only id band.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecallStatsTimeZoneRepairMigrationTest {

  @Autowired DataSource dataSource;

  @Test
  void doesNotTouchRowsWhenPlaceholderDefaultsToNoOp() throws Exception {
    String migrationSql =
        StreamUtils.copyToString(
            new ClassPathResource(
                    "db/migration/V300000233__repair_tz_skewed_quiz_answer_created_at.sql")
                .getInputStream(),
            StandardCharsets.UTF_8);
    String resolvedSql = migrationSql.replace("${tz_repair}", "1=0");

    long originalEpochMillis = Timestamp.valueOf("2025-08-15 08:00:00").getTime();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      long insertedId;
      try (PreparedStatement insert =
          connection.prepareStatement(
              "INSERT INTO quiz_answer (result, created_at, correct) VALUES (0, ?, 0)")) {
        insert.setTimestamp(1, new Timestamp(originalEpochMillis));
        insert.executeUpdate();
      }
      try (Statement idQuery = connection.createStatement()) {
        ResultSet inserted = idQuery.executeQuery("SELECT LAST_INSERT_ID()");
        inserted.next();
        insertedId = inserted.getLong(1);
      }

      try (Statement repair = connection.createStatement()) {
        int updatedRows = repair.executeUpdate(resolvedSql);
        assertThat(updatedRows, is(0));
      }

      try (PreparedStatement select =
          connection.prepareStatement("SELECT created_at FROM quiz_answer WHERE id = ?")) {
        select.setLong(1, insertedId);
        ResultSet after = select.executeQuery();
        after.next();
        assertThat(after.getTimestamp(1).getTime(), is(originalEpochMillis));
      }
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
