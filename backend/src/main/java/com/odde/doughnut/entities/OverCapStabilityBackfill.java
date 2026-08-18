package com.odde.doughnut.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class OverCapStabilityBackfill {

  private OverCapStabilityBackfill() {}

  public static void run(Connection connection) throws SQLException {
    float cap = Fsrs.MAXIMUM_INTERVAL_HOURS;
    int intervalHours = Fsrs.intervalHours(cap);
    try (PreparedStatement update =
        connection.prepareStatement(
            """
            UPDATE memory_tracker
            SET stability = LEAST(stability, ?),
                next_recall_at = TIMESTAMPADD(HOUR, ?, last_recalled_at)
            WHERE stability > ?
            """)) {
      update.setFloat(1, cap);
      update.setInt(2, intervalHours);
      update.setFloat(3, cap);
      update.executeUpdate();
    }
  }
}
