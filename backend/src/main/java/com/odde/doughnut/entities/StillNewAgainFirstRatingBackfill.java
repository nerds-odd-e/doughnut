package com.odde.doughnut.entities;

import java.sql.Connection;
import java.sql.SQLException;

public final class StillNewAgainFirstRatingBackfill {

  private StillNewAgainFirstRatingBackfill() {}

  public static void run(Connection connection, String gate) throws SQLException {
    StillNewFirstRatingBackfill.runAgain(connection, gate);
  }
}
