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
 * baseline construction. Used by {@link NotebookGitBaselineRebuild} because Flyway migrations run
 * before a JPA persistence context exists.
 *
 * <p>Migration-owned and temporary: retained only to support the notebook Git-binding rebaseline
 * migration and meant for removal once that migration is confirmed applied.
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

  // Mirrors NoteRepository#findLiveNotesByNotebookIdOrderByIdAsc (same ordering) for the same
  // reason as FOLDERS_QUERY above. The note table has no soft-deletion column, so every note row,
  // including notes located under a _trash folder subtree, belongs to the Portable tree.
  private static final String NOTES_QUERY =
      """
      SELECT folder_id, title, content
      FROM note
      WHERE notebook_id = ?
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

  static Integer readNullableInt(ResultSet resultSet, String column) throws SQLException {
    int value = resultSet.getInt(column);
    return resultSet.wasNull() ? null : value;
  }
}
