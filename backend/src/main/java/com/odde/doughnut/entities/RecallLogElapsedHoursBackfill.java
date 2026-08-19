package com.odde.doughnut.entities;

import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecallLogElapsedHoursBackfill {

  private RecallLogElapsedHoursBackfill() {}

  public static void run(Connection connection) throws SQLException {
    Map<Integer, Integer> updates = reconstructedNullElapsedById(loadLogs(connection));
    try (PreparedStatement update =
        connection.prepareStatement(
            "UPDATE recall_log SET elapsed_hours = ? WHERE id = ? AND elapsed_hours IS NULL")) {
      for (var entry : updates.entrySet()) {
        update.setInt(1, entry.getValue());
        update.setInt(2, entry.getKey());
        update.executeUpdate();
      }
    }
  }

  static Map<Integer, Integer> reconstructedNullElapsedById(List<LogRow> rows) {
    Map<Integer, Integer> updates = new LinkedHashMap<>();
    Integer currentTrackerId = null;
    Timestamp lastMappedAt = null;
    for (LogRow row : rows) {
      if (currentTrackerId == null || row.trackerId() != currentTrackerId) {
        currentTrackerId = row.trackerId();
        lastMappedAt = null;
      }
      if (row.elapsedHours() == null) {
        updates.put(row.id(), reconstructedElapsedHours(row.recordedAt(), lastMappedAt));
      }
      if (row.productOutcome().isMappedGrade()) {
        lastMappedAt = row.recordedAt();
      }
    }
    return updates;
  }

  private static List<LogRow> loadLogs(Connection connection) throws SQLException {
    List<LogRow> rows = new ArrayList<>();
    try (PreparedStatement select =
            connection.prepareStatement(
                """
                SELECT id, memory_tracker_id, recorded_at, product_outcome, elapsed_hours
                FROM recall_log
                ORDER BY memory_tracker_id, recorded_at, id
                """);
        ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        rows.add(
            new LogRow(
                rs.getInt("id"),
                rs.getInt("memory_tracker_id"),
                rs.getTimestamp("recorded_at"),
                ProductOutcome.valueOf(rs.getString("product_outcome")),
                rs.getObject("elapsed_hours", Integer.class)));
      }
    }
    return rows;
  }

  private static int reconstructedElapsedHours(Timestamp recordedAt, Timestamp lastMappedAt) {
    if (lastMappedAt == null) {
      return 0;
    }
    return (int) Math.max(0L, TimestampOperations.getDiffInHours(recordedAt, lastMappedAt));
  }

  record LogRow(
      int id,
      int trackerId,
      Timestamp recordedAt,
      ProductOutcome productOutcome,
      Integer elapsedHours) {}
}
