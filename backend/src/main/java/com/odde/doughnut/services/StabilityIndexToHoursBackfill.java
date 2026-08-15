package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/** Convert persisted Stability from legacy index scale to whole hours, then drop day lists. */
public final class StabilityIndexToHoursBackfill {

  private StabilityIndexToHoursBackfill() {}

  public static void run(Connection connection) throws SQLException {
    convertStabilityToHours(connection);
    try (Statement ddl = connection.createStatement()) {
      ddl.execute("ALTER TABLE memory_tracker MODIFY COLUMN stability float NOT NULL DEFAULT '0'");
      ddl.execute("ALTER TABLE user DROP COLUMN space_intervals");
    }
  }

  private static void convertStabilityToHours(Connection connection) throws SQLException {
    try (PreparedStatement select =
            connection.prepareStatement(
                """
                SELECT mt.id, mt.stability, u.space_intervals
                FROM memory_tracker mt
                INNER JOIN user u ON mt.user_id = u.id
                """);
        PreparedStatement update =
            connection.prepareStatement("UPDATE memory_tracker SET stability = ? WHERE id = ?");
        ResultSet rows = select.executeQuery()) {
      while (rows.next()) {
        float legacyIndex = rows.getFloat("stability");
        int hours =
            SpacedRepetitionAlgorithm.hoursFromLegacyIndex(
                legacyIndex, parseDayList(rows.getString("space_intervals")));
        update.setFloat(1, hours);
        update.setInt(2, rows.getInt("id"));
        update.addBatch();
      }
      update.executeBatch();
    }
  }

  private static List<Integer> parseDayList(String spaceIntervals) {
    if (spaceIntervals == null || spaceIntervals.isEmpty()) {
      return List.of();
    }
    return Arrays.stream(spaceIntervals.split(",\\s*")).map(Integer::valueOf).toList();
  }
}
