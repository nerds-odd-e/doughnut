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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemoryTrackerRecallAnchorRepairMigrationTest {

  private static final String NORMAL_ANSWER_REPAIR_MIGRATION =
      "db/migration/V300000248__repair_memory_tracker_recall_anchor_from_answers.sql";

  @Autowired DataSource dataSource;
  private Connection connection;

  @BeforeEach
  void acquireConnection() {
    connection = DataSourceUtils.getConnection(dataSource);
  }

  @AfterEach
  void releaseConnection() {
    DataSourceUtils.releaseConnection(connection, dataSource);
  }

  @Test
  void doesNotRepairAnchorsWhenPlaceholderDefaultsToNoOp() throws Exception {
    Timestamp originalAnchor = timestamp("2026-01-01 08:00:00");
    long trackerId = insertMemoryTracker(originalAnchor, timestamp("2026-02-01 08:00:00"));
    insertAnswer(trackerId, timestamp("2026-01-03 08:00:00"), true, null);

    assertThat(runNormalAnswerRepair("1=0"), is(0));

    assertThat(readTrackerState(trackerId).lastRecalledAt(), is(originalAnchor));
  }

  @Test
  void repairsToLatestNormalAnswerWithoutChangingDueAndIsIdempotent() throws Exception {
    Timestamp originalAnchor = timestamp("2026-01-01 08:00:00");
    Timestamp due = timestamp("2026-02-01 08:00:00");
    Timestamp latestNormalAnswer = timestamp("2026-01-03 08:00:00");
    long trackerId = insertMemoryTracker(originalAnchor, due);
    insertAnswer(trackerId, timestamp("2026-01-02 08:00:00"), true, null);
    insertAnswer(trackerId, latestNormalAnswer, false, null);
    insertAnswer(trackerId, timestamp("2026-01-04 08:00:00"), true, "OVERLAP");

    assertThat(runNormalAnswerRepair("1=1"), is(1));
    TrackerState repaired = readTrackerState(trackerId);
    assertThat(repaired.lastRecalledAt(), is(latestNormalAnswer));
    assertThat(repaired.nextRecallAt(), is(due));

    assertThat(runNormalAnswerRepair("1=1"), is(0));
    assertThat(readTrackerState(trackerId), is(repaired));
  }

  @Test
  void preservesAnAnchorLaterThanTheLatestNormalAnswer() throws Exception {
    Timestamp currentAnchor = timestamp("2026-01-05 08:00:00");
    long trackerId = insertMemoryTracker(currentAnchor, timestamp("2026-02-01 08:00:00"));
    insertAnswer(trackerId, timestamp("2026-01-03 08:00:00"), true, null);

    assertThat(runNormalAnswerRepair("1=1"), is(0));

    assertThat(readTrackerState(trackerId).lastRecalledAt(), is(currentAnchor));
  }

  private int runNormalAnswerRepair(String placeholder) throws Exception {
    String migrationSql =
        StreamUtils.copyToString(
            new ClassPathResource(NORMAL_ANSWER_REPAIR_MIGRATION).getInputStream(),
            StandardCharsets.UTF_8);
    try (Statement repair = connection.createStatement()) {
      return repair.executeUpdate(migrationSql.replace("${recall_anchor_repair}", placeholder));
    }
  }

  private long insertMemoryTracker(Timestamp anchor, Timestamp due) throws Exception {
    long userId = insertUser();
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO memory_tracker (user_id, last_recalled_at, next_recall_at) VALUES (?, ?, ?)")) {
      insert.setLong(1, userId);
      insert.setTimestamp(2, anchor);
      insert.setTimestamp(3, due);
      insert.executeUpdate();
    }
    return lastInsertId();
  }

  private long insertUser() throws Exception {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO user (name, external_identifier) VALUES ('recall-anchor-repair-test', ?)")) {
      insert.setString(1, "recall-anchor-repair-test-" + System.nanoTime());
      insert.executeUpdate();
    }
    return lastInsertId();
  }

  private void insertAnswer(long trackerId, Timestamp answeredAt, boolean correct, String outcome)
      throws Exception {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO quiz_answer (result, created_at, correct, outcome) VALUES (0, ?, ?, ?)")) {
      insert.setTimestamp(1, answeredAt);
      insert.setBoolean(2, correct);
      insert.setString(3, outcome);
      insert.executeUpdate();
    }
    long answerId = lastInsertId();
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO recall_prompt (memory_tracker_id, question_type, quiz_answer_id) VALUES (?, 'SPELLING', ?)")) {
      insert.setLong(1, trackerId);
      insert.setLong(2, answerId);
      insert.executeUpdate();
    }
  }

  private TrackerState readTrackerState(long trackerId) throws Exception {
    try (PreparedStatement select =
        connection.prepareStatement(
            "SELECT last_recalled_at, next_recall_at FROM memory_tracker WHERE id = ?")) {
      select.setLong(1, trackerId);
      ResultSet result = select.executeQuery();
      result.next();
      return new TrackerState(result.getTimestamp(1), result.getTimestamp(2));
    }
  }

  private long lastInsertId() throws Exception {
    try (Statement query = connection.createStatement()) {
      ResultSet result = query.executeQuery("SELECT LAST_INSERT_ID()");
      result.next();
      return result.getLong(1);
    }
  }

  private Timestamp timestamp(String value) {
    return Timestamp.valueOf(value);
  }

  private record TrackerState(Timestamp lastRecalledAt, Timestamp nextRecallAt) {}
}
