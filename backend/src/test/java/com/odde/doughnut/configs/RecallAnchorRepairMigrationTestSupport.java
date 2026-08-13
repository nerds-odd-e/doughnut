package com.odde.doughnut.configs;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
abstract class RecallAnchorRepairMigrationTestSupport {

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

  int runRepair(String migrationResource, String placeholder) throws Exception {
    String migrationSql =
        StreamUtils.copyToString(
            new ClassPathResource(migrationResource).getInputStream(), StandardCharsets.UTF_8);
    try (Statement repair = connection.createStatement()) {
      return repair.executeUpdate(migrationSql.replace("${recall_anchor_repair}", placeholder));
    }
  }

  long insertMemoryTracker(Timestamp anchor, Timestamp due) throws Exception {
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

  void insertAnswer(long trackerId, Timestamp answeredAt, boolean correct, String outcome)
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

  void insertTutorFeedback(long trackerId, Timestamp feedbackRecordedAt) throws Exception {
    long userId = insertUser();
    long notebookId = insertNotebook(userId);
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO learning_session (user_id, notebook_id, recorded_at) VALUES (?, ?, ?)")) {
      insert.setLong(1, userId);
      insert.setLong(2, notebookId);
      insert.setTimestamp(3, feedbackRecordedAt);
      insert.executeUpdate();
    }
    long learningSessionId = lastInsertId();
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO session_item (learning_session_id, memory_tracker_id, note_title, feedback_score, feedback_recorded_at) VALUES (?, ?, 'recall-anchor-repair-test', 3, ?)")) {
      insert.setLong(1, learningSessionId);
      insert.setLong(2, trackerId);
      insert.setTimestamp(3, feedbackRecordedAt);
      insert.executeUpdate();
    }
  }

  TrackerState readTrackerState(long trackerId) throws Exception {
    try (PreparedStatement select =
        connection.prepareStatement(
            "SELECT last_recalled_at, next_recall_at FROM memory_tracker WHERE id = ?")) {
      select.setLong(1, trackerId);
      ResultSet result = select.executeQuery();
      result.next();
      return new TrackerState(result.getTimestamp(1), result.getTimestamp(2));
    }
  }

  Timestamp timestamp(String value) {
    return Timestamp.valueOf(value);
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

  private long insertNotebook(long userId) throws Exception {
    try (PreparedStatement insert =
        connection.prepareStatement("INSERT INTO ownership (user_id) VALUES (?)")) {
      insert.setLong(1, userId);
      insert.executeUpdate();
    }
    long ownershipId = lastInsertId();
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO notebook (ownership_id, creator_id, name) VALUES (?, ?, 'recall-anchor-repair-test')")) {
      insert.setLong(1, ownershipId);
      insert.setLong(2, userId);
      insert.executeUpdate();
    }
    return lastInsertId();
  }

  private long lastInsertId() throws Exception {
    try (Statement query = connection.createStatement()) {
      ResultSet result = query.executeQuery("SELECT LAST_INSERT_ID()");
      result.next();
      return result.getLong(1);
    }
  }

  record TrackerState(Timestamp lastRecalledAt, Timestamp nextRecallAt) {}
}
