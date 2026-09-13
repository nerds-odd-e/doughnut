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
    String notebookReadmeContent = NotebookGitRows.readNotebookReadme(connection, notebookId);
    List<ExportFolderRow> folders = NotebookGitRows.readFolders(connection, notebookId);
    List<ExportNoteRow> notes = NotebookGitRows.readNotes(connection, notebookId);
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
}
