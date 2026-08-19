package com.odde.doughnut.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Unique-key-aware JDBC conversion of note, folder, and notebook display names. */
final class DisplayNameUniqueKeyJdbcConversion {

  private DisplayNameUniqueKeyJdbcConversion() {}

  @FunctionalInterface
  interface ConflictFailure {
    IllegalStateException conflict(
        String table,
        int updatingId,
        int conflictingId,
        String rawValue,
        String conflictingRawValue,
        String normalized,
        String uniqueKey);
  }

  @FunctionalInterface
  interface BlankFailure {
    IllegalStateException blank(String table, int id, String rawValue, String uniqueKey);
  }

  static void convertNoteFolderAndNotebookNames(
      Connection connection, Function<String, String> convert, ConflictFailure conflictFailure)
      throws SQLException {
    convertNoteFolderAndNotebookNames(connection, convert, null, conflictFailure);
  }

  static void convertNoteFolderAndNotebookNames(
      Connection connection,
      Function<String, String> convert,
      BlankFailure blankFailure,
      ConflictFailure conflictFailure)
      throws SQLException {
    convertTable(
        connection,
        convert,
        blankFailure,
        conflictFailure,
        "SELECT id, notebook_id, folder_id, title FROM note",
        "UPDATE note SET title = ? WHERE id = ?",
        "note",
        "uk_note_notebook_folder_title",
        DisplayNameUniqueKeyJdbcConversion::caseInsensitiveKey,
        rs ->
            new Row(
                rs.getInt("id"),
                rs.getString("title"),
                nullToZero(nullableInt(rs, "notebook_id")),
                nullToZero(nullableInt(rs, "folder_id"))));
    convertTable(
        connection,
        convert,
        blankFailure,
        conflictFailure,
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
    convertTable(
        connection,
        convert,
        blankFailure,
        conflictFailure,
        "SELECT id, ownership_id, name FROM notebook",
        "UPDATE notebook SET name = ? WHERE id = ?",
        "notebook",
        "uk_notebook_ownership_name",
        Function.identity(),
        rs -> new Row(rs.getInt("id"), rs.getString("name"), rs.getInt("ownership_id"), 0));
  }

  private static void convertTable(
      Connection connection,
      Function<String, String> convert,
      BlankFailure blankFailure,
      ConflictFailure conflictFailure,
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
        String normalized = convert.apply(row.name());
        if (Objects.equals(normalized, row.name())) {
          continue;
        }
        if (blankFailure != null && normalized.isEmpty()) {
          throw blankFailure.blank(table, row.id(), row.name(), uniqueKey);
        }
        String targetKey = uniquenessKey.apply(normalized);
        for (Row other : rows) {
          if (other.id() == row.id() || !row.sameScope(other)) {
            continue;
          }
          if (uniquenessKey.apply(convert.apply(other.name())).equals(targetKey)) {
            throw conflictFailure.conflict(
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
