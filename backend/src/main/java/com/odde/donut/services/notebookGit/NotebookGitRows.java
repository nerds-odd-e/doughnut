package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a notebook's current content (readme, folders, live notes) as raw-JDBC export rows for Git
 * baseline construction. Shared by the pre-JPA, migration-capable operations in this package
 * ({@link NotebookGitFleetCutoverBackfill} and {@link NotebookGitBaselineRebuild}) so the SQL
 * shapes and row mapping live in one place.
 */
public final class NotebookGitRows {

  private static final String NOTEBOOK_README_QUERY =
      "SELECT readme_content FROM notebook WHERE id = ?";

  // Mirrors FolderRepository#findByNotebookIdOrderByIdAsc, which NotebookExportRows uses on the
  // JPA path (NotebookGitCutoverService). No JPA context exists yet at migration time, so keep
  // this filter/order in sync by hand if that repository query ever changes.
  private static final String FOLDERS_QUERY =
      """
      SELECT id, parent_folder_id, name, readme_content
      FROM folder
      WHERE notebook_id = ?
      ORDER BY id ASC
      """;

  // Mirrors NoteRepository#findLiveNotesByNotebookIdOrderByIdAsc (same deleted_at filter and
  // ordering) for the same reason as FOLDERS_QUERY above. Live notes include location-based trash
  // (notes under a _trash folder subtree with deleted_at IS NULL), so the Portable tree naturally
  // contains trash.
  private static final String NOTES_QUERY =
      """
      SELECT folder_id, title, content
      FROM note
      WHERE notebook_id = ? AND deleted_at IS NULL
      ORDER BY id ASC
      """;

  private NotebookGitRows() {}

  static String readNotebookReadme(Connection connection, int notebookId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(NOTEBOOK_README_QUERY)) {
      statement.setInt(1, notebookId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getString("readme_content");
      }
    }
  }

  static List<ExportFolderRow> readFolders(Connection connection, int notebookId)
      throws SQLException {
    List<ExportFolderRow> folders = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(FOLDERS_QUERY)) {
      statement.setInt(1, notebookId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          folders.add(
              new ExportFolderRow(
                  resultSet.getInt("id"),
                  readNullableInt(resultSet, "parent_folder_id"),
                  resultSet.getString("name"),
                  resultSet.getString("readme_content")));
        }
      }
    }
    return folders;
  }

  static List<ExportNoteRow> readNotes(Connection connection, int notebookId) throws SQLException {
    List<ExportNoteRow> notes = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(NOTES_QUERY)) {
      statement.setInt(1, notebookId);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          notes.add(
              new ExportNoteRow(
                  readNullableInt(resultSet, "folder_id"),
                  resultSet.getString("title"),
                  resultSet.getString("content")));
        }
      }
    }
    return notes;
  }

  private static Integer readNullableInt(ResultSet resultSet, String column) throws SQLException {
    int value = resultSet.getInt(column);
    return resultSet.wasNull() ? null : value;
  }
}
