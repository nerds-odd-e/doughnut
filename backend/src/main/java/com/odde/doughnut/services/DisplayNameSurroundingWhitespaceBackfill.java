package com.odde.doughnut.services;

import com.odde.doughnut.entities.DisplayName;
import java.sql.Connection;
import java.sql.SQLException;

/** One-shot JDBC backfill: trim surrounding whitespace on note/folder/notebook display names. */
public final class DisplayNameSurroundingWhitespaceBackfill {

  private DisplayNameSurroundingWhitespaceBackfill() {}

  public static void run(Connection connection) throws SQLException {
    DisplayNameUniqueKeyJdbcConversion.convertNoteFolderAndNotebookNames(
        connection,
        name -> new DisplayName(name).value(),
        DisplayNameSurroundingWhitespaceBackfill::conflict);
  }

  private static IllegalStateException conflict(
      String table,
      int updatingId,
      int conflictingId,
      String rawValue,
      String conflictingRawValue,
      String normalized,
      String uniqueKey) {
    return new IllegalStateException(
        "Display-name whitespace backfill aborted: trimming would violate "
            + uniqueKey
            + " on "
            + table
            + " id="
            + updatingId
            + " raw=["
            + rawValue
            + "] normalized=["
            + normalized
            + "] conflicts with id="
            + conflictingId
            + " raw=["
            + conflictingRawValue
            + "]");
  }
}
