package com.odde.doughnut.entities;

import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class RecallLogElapsedHoursBackfill {

  private RecallLogElapsedHoursBackfill() {}

  public static void run(Connection connection) throws SQLException {
    List<LogRow> rows = loadLogs(connection);
    try (PreparedStatement update =
        connection.prepareStatement(
            "UPDATE recall_log SET elapsed_hours = ? WHERE id = ? AND elapsed_hours IS NULL")) {
      Integer currentTrackerId = null;
      Timestamp lastMappedAt = null;
      for (LogRow row : rows) {
        if (currentTrackerId == null || row.trackerId() != currentTrackerId) {
          currentTrackerId = row.trackerId();
          lastMappedAt = null;
        }
        if (row.elapsedHours() == null) {
          update.setInt(1, reconstructedElapsedHours(row.recordedAt(), lastMappedAt));
          update.setInt(2, row.id());
          update.executeUpdate();
        }
        if (row.productOutcome().isMappedGrade()) {
          lastMappedAt = row.recordedAt();
        }
      }
    }
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

  private record LogRow(
      int id,
      int trackerId,
      Timestamp recordedAt,
      ProductOutcome productOutcome,
      Integer elapsedHours) {}
}
