package com.odde.doughnut.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class AliasRecallLogGradeBackfill {

  private AliasRecallLogGradeBackfill() {}

  public static void run(Connection connection) throws SQLException {
    try (PreparedStatement update =
        connection.prepareStatement(
            """
            UPDATE recall_log
            SET product_outcome = CASE product_outcome
              WHEN 'SHRINK' THEN 'HARD'
              WHEN 'AGAIN_ZERO' THEN 'AGAIN'
            END
            WHERE product_outcome IN ('SHRINK', 'AGAIN_ZERO')
            """)) {
      update.executeUpdate();
    }
  }
}
