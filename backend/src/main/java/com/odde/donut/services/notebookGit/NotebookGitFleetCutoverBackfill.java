package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.Repository;

/**
 * One-time, raw-JDBC fleet backfill that gives every live pre-Git notebook its accepted Git
 * binding. Mirrors {@link NotebookGitCutoverService}'s per-notebook snapshot/bundle construction
 * without any JPA/Spring dependency, so it can run from a Flyway {@code BaseJavaMigration} before
 * the application's {@code EntityManagerFactory} is available.
 *
 * <p>Idempotent: {@link #candidateNotebookIds(Connection)} only returns live notebooks with no
 * existing {@code notebook_git_binding} row, so re-running never creates a second binding. Each
 * notebook's root commit and binding row are committed independently, so a failure partway through
 * a run leaves already-bound notebooks intact for a safe retry.
 */
public final class NotebookGitFleetCutoverBackfill {

  private static final String CANDIDATE_NOTEBOOKS_QUERY =
      """
      SELECT n.id
      FROM notebook n
      LEFT JOIN notebook_git_binding b ON b.notebook_id = n.id
      WHERE n.deleted_at IS NULL AND b.id IS NULL
      ORDER BY n.id ASC
      """;

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
  // ordering) for the same reason as FOLDERS_QUERY above.
  private static final String NOTES_QUERY =
      """
      SELECT folder_id, title, content
      FROM note
      WHERE notebook_id = ? AND deleted_at IS NULL
      ORDER BY id ASC
      """;

  private static final String INSERT_BINDING =
      """
      INSERT INTO notebook_git_binding
        (notebook_id, accepted_git_object_id, bundle_bytes, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?)
      """;

  private NotebookGitFleetCutoverBackfill() {}

  /**
   * Backfills every candidate notebook, committing each notebook's binding independently before
   * moving to the next. Restores the connection's original auto-commit mode when done (or on
   * failure).
   */
  public static void run(Connection connection, Instant cutoverTime) throws SQLException {
    boolean originalAutoCommit = connection.getAutoCommit();
    connection.setAutoCommit(false);
    try {
      for (int notebookId : candidateNotebookIds(connection)) {
        bindNotebook(connection, notebookId, cutoverTime);
        connection.commit();
      }
    } finally {
      connection.setAutoCommit(originalAutoCommit);
    }
  }

  static List<Integer> candidateNotebookIds(Connection connection) throws SQLException {
    List<Integer> notebookIds = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(CANDIDATE_NOTEBOOKS_QUERY);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        notebookIds.add(resultSet.getInt("id"));
      }
    }
    return notebookIds;
  }

  static void bindNotebook(Connection connection, int notebookId, Instant cutoverTime)
      throws SQLException {
    String notebookReadmeContent = readNotebookReadme(connection, notebookId);
    List<ExportFolderRow> folders = readFolders(connection, notebookId);
    List<ExportNoteRow> notes = readNotes(connection, notebookId);
    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(notebookReadmeContent, folders, notes);

    try (Repository gitRepository =
        NotebookGitBundleBuilder.build(
            entries,
            NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
            NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
            NotebookGitCutoverService.CUTOVER_COMMIT_MESSAGE,
            cutoverTime)) {
      NotebookGitBundleWriter.BundleWriteResult written =
          NotebookGitBundleWriter.write(gitRepository);
      insertBinding(connection, notebookId, written, cutoverTime);
    }
  }

  private static String readNotebookReadme(Connection connection, int notebookId)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(NOTEBOOK_README_QUERY)) {
      statement.setInt(1, notebookId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getString("readme_content");
      }
    }
  }

  private static List<ExportFolderRow> readFolders(Connection connection, int notebookId)
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

  private static List<ExportNoteRow> readNotes(Connection connection, int notebookId)
      throws SQLException {
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

  private static void insertBinding(
      Connection connection,
      int notebookId,
      NotebookGitBundleWriter.BundleWriteResult written,
      Instant cutoverTime)
      throws SQLException {
    Timestamp cutoverTimestamp = Timestamp.from(cutoverTime);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_BINDING)) {
      statement.setInt(1, notebookId);
      statement.setString(2, written.headObjectId());
      statement.setBytes(3, written.bundleBytes());
      statement.setTimestamp(4, cutoverTimestamp);
      statement.setTimestamp(5, cutoverTimestamp);
      statement.executeUpdate();
    }
  }

  private static Integer readNullableInt(ResultSet resultSet, String column) throws SQLException {
    int value = resultSet.getInt(column);
    return resultSet.wasNull() ? null : value;
  }
}
