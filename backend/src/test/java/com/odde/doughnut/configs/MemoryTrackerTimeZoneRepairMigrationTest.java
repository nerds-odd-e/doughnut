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
 * db/migration/V300000234__repair_tz_skewed_memory_tracker_scheduling.sql): with the no-op default
 * (`tz_repair=1=0`, matching every non-prod profile), neither UPDATE may touch a row, even one
 * whose timestamps fall inside the repaired date band, because a fresh test row's id and linked
 * quiz_answer id fall outside the literal prod-only bands.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemoryTrackerTimeZoneRepairMigrationTest {

  @Autowired DataSource dataSource;

  @Test
  void doesNotTouchRowsWhenPlaceholderDefaultsToNoOp() throws Exception {
    String migrationSql =
        StreamUtils.copyToString(
            new ClassPathResource(
                    "db/migration/V300000234__repair_tz_skewed_memory_tracker_scheduling.sql")
                .getInputStream(),
            StandardCharsets.UTF_8);
    String[] statements = migrationSql.replace("${tz_repair}", "1=0").split(";");

    long originalEpochMillis = Timestamp.valueOf("2025-08-15 08:00:00").getTime();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      long userId = insertUser(connection);
      long trackerId = insertMemoryTracker(connection, userId, originalEpochMillis);
      insertRecallPromptWithAnswer(connection, trackerId, originalEpochMillis);

      try (Statement repair = connection.createStatement()) {
        for (String statement : statements) {
          if (!statement.isBlank()) {
            int updatedRows = repair.executeUpdate(statement);
            assertThat(updatedRows, is(0));
          }
        }
      }

      try (PreparedStatement select =
          connection.prepareStatement(
              "SELECT last_recalled_at, next_recall_at, assimilated_at FROM memory_tracker WHERE id = ?")) {
        select.setLong(1, trackerId);
        ResultSet after = select.executeQuery();
        after.next();
        assertThat(after.getTimestamp(1).getTime(), is(originalEpochMillis));
        assertThat(after.getTimestamp(2).getTime(), is(originalEpochMillis));
        assertThat(after.getTimestamp(3).getTime(), is(originalEpochMillis));
      }
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private long insertUser(Connection connection) throws Exception {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO user (name, external_identifier) VALUES ('tz-repair-test', ?)")) {
      insert.setString(1, "tz-repair-test-" + System.nanoTime());
      insert.executeUpdate();
    }
    return lastInsertId(connection);
  }

  private long insertMemoryTracker(Connection connection, long userId, long epochMillis)
      throws Exception {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO memory_tracker (user_id, last_recalled_at, assimilated_at, next_recall_at)"
                + " VALUES (?, ?, ?, ?)")) {
      insert.setLong(1, userId);
      insert.setTimestamp(2, new Timestamp(epochMillis));
      insert.setTimestamp(3, new Timestamp(epochMillis));
      insert.setTimestamp(4, new Timestamp(epochMillis));
      insert.executeUpdate();
    }
    return lastInsertId(connection);
  }

  private void insertRecallPromptWithAnswer(Connection connection, long trackerId, long epochMillis)
      throws Exception {
    long quizAnswerId;
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO quiz_answer (result, created_at, correct) VALUES (0, ?, 0)")) {
      insert.setTimestamp(1, new Timestamp(epochMillis));
      insert.executeUpdate();
    }
    quizAnswerId = lastInsertId(connection);

    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO recall_prompt (memory_tracker_id, question_type, quiz_answer_id) VALUES (?, 'SPELLING', ?)")) {
      insert.setLong(1, trackerId);
      insert.setLong(2, quizAnswerId);
      insert.executeUpdate();
    }
  }

  private long lastInsertId(Connection connection) throws Exception {
    try (Statement idQuery = connection.createStatement()) {
      ResultSet result = idQuery.executeQuery("SELECT LAST_INSERT_ID()");
      result.next();
      return result.getLong(1);
    }
  }
}
