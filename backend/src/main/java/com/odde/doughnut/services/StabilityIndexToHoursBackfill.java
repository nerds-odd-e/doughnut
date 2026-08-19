package com.odde.doughnut.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/** Convert persisted Stability from legacy index scale to whole hours, then drop day lists. */
public final class StabilityIndexToHoursBackfill {
  private static final List<Integer> DEFAULT_SPACES =
      Arrays.asList(
          0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765,
          10946, 17711, 28657, 46368, 75025);

  private static final float LEGACY_INDEX_OFFSET = 100.0f;
  private static final float LEGACY_INDEX_STEP = 10.0f;

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
            hoursFromLegacyIndex(legacyIndex, parseDayList(rows.getString("space_intervals")));
        update.setFloat(1, hours);
        update.setInt(2, rows.getInt("id"));
        update.addBatch();
      }
      update.executeBatch();
    }
  }

  static int hoursFromLegacyIndex(float legacyIndex, List<Integer> spaces) {
    float spacingIndex = (legacyIndex - LEGACY_INDEX_OFFSET) / LEGACY_INDEX_STEP;
    return hoursFromSpacingIndex(spacingIndex, spaces);
  }

  private static int hoursFromSpacingIndex(float spacingIndex, List<Integer> spaces) {
    if (spacingIndex < 0) {
      return 0;
    }
    int floor = spacingDays((int) spacingIndex, spaces);
    int ceiling = spacingDays((int) spacingIndex + 1, spaces);
    return (int) (floor * 24 + (ceiling - floor) * 24 * (spacingIndex - (int) spacingIndex));
  }

  private static int spacingDays(int index, List<Integer> spaces) {
    if (index < spaces.size()) {
      return spaces.get(index);
    }
    int defaultIndex = Math.min(index, DEFAULT_SPACES.size() - 1);
    return DEFAULT_SPACES.get(defaultIndex);
  }

  private static List<Integer> parseDayList(String spaceIntervals) {
    if (spaceIntervals == null || spaceIntervals.isEmpty()) {
      return List.of();
    }
    return Arrays.stream(spaceIntervals.split(",\\s*")).map(Integer::valueOf).toList();
  }
}
