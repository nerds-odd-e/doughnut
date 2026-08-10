package com.odde.doughnut.services;

import com.odde.doughnut.entities.DisplayName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** One-shot JDBC backfill: trim surrounding whitespace on note/folder/notebook display names. */
public final class DisplayNameSurroundingWhitespaceBackfill {

  private DisplayNameSurroundingWhitespaceBackfill() {}

  public static void run(Connection connection) throws SQLException {
    normalize(
        connection,
        "SELECT id, notebook_id, folder_id, title FROM note",
        "UPDATE note SET title = ? WHERE id = ?",
        "note",
        "uk_note_notebook_folder_title",
        DisplayNameSurroundingWhitespaceBackfill::caseInsensitiveKey,
        rs ->
            new Row(
                rs.getInt("id"),
                rs.getString("title"),
                nullToZero(nullableInt(rs, "notebook_id")),
                nullToZero(nullableInt(rs, "folder_id"))));
    normalize(
        connection,
        "SELECT id, notebook_id, parent_folder_id, name FROM folder",
        "UPDATE folder SET name = ? WHERE id = ?",
        "folder",
        "uk_folder_notebook_parent_name",
        Function.identity(),
        rs ->
            new Row(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("notebook_id"),
                nullToZero(nullableInt(rs, "parent_folder_id"))));
    normalize(
        connection,
        "SELECT id, ownership_id, name FROM notebook",
        "UPDATE notebook SET name = ? WHERE id = ?",
        "notebook",
        "uk_notebook_ownership_name",
        Function.identity(),
        rs -> new Row(rs.getInt("id"), rs.getString("name"), rs.getInt("ownership_id"), 0));
  }

  private static void normalize(
      Connection connection,
      String selectSql,
      String updateSql,
      String table,
      String uniqueKey,
      Function<String, String> uniquenessKey,
      RowReader reader)
      throws SQLException {
    List<Row> rows = new ArrayList<>();
    try (PreparedStatement select = connection.prepareStatement(selectSql);
        ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        rows.add(reader.read(rs));
      }
    }

    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      for (Row row : rows) {
        String normalized = new DisplayName(row.name()).value();
        if (Objects.equals(normalized, row.name())) {
          continue;
        }
        String targetKey = uniquenessKey.apply(normalized);
        for (Row other : rows) {
          if (other.id() == row.id() || !row.sameScope(other)) {
            continue;
          }
          if (uniquenessKey.apply(new DisplayName(other.name()).value()).equals(targetKey)) {
            throw conflict(
                table, row.id(), other.id(), row.name(), other.name(), normalized, uniqueKey);
          }
        }
        update.setString(1, normalized);
        update.setInt(2, row.id());
        update.executeUpdate();
      }
    }
  }

  private static int nullToZero(Integer value) {
    return value == null ? 0 : value;
  }

  private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private static String caseInsensitiveKey(String title) {
    return title.toLowerCase(Locale.ROOT);
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

  @FunctionalInterface
  private interface RowReader {
    Row read(ResultSet rs) throws SQLException;
  }

  private record Row(int id, String name, int scopeA, int scopeB) {
    boolean sameScope(Row other) {
      return scopeA == other.scopeA && scopeB == other.scopeB;
    }
  }
}
