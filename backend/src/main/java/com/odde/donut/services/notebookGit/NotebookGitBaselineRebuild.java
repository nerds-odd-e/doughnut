package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.Repository;

/**
 * One-time, raw-JDBC baseline rebuild that replaces an existing notebook Git binding with a fresh
 * single-root commit built from the notebook's CURRENT database content. Targets a notebook that
 * ALREADY has a {@code notebook_git_binding} row and UPDATEs its accepted head/bundle in place —
 * retaining the binding's id, notebook ownership, and {@code created_at}.
 *
 * <p>Use this to replace a notebook's accepted history with a clean root snapshot of current
 * content. The replacement has exactly one parentless root commit; the previous base and every
 * later accepted commit are abandoned without a backup or ancestry bridge. No destructive
 * delete-and-reinsert of user data occurs.
 *
 * <p>Migration-owned and JDBC-capable so it can run from a Flyway {@code BaseJavaMigration} before
 * the application's {@code EntityManagerFactory} is available. No automatic upgrade is registered
 * here; callers wire the validated recipe when needed. Temporary: retained only to support the
 * notebook Git-binding rebaseline migration and meant for removal once that migration is confirmed
 * applied.
 */
public final class NotebookGitBaselineRebuild {

  private static final String UPDATE_BINDING =
      """
      UPDATE notebook_git_binding
      SET accepted_git_object_id = ?, bundle_bytes = ?, updated_at = ?
      WHERE notebook_id = ?
      """;

  private NotebookGitBaselineRebuild() {}

  /**
   * Rebuilds the accepted Git baseline for a single notebook that already has a binding row.
   * Commits the replacement atomically. Restores the connection's original auto-commit mode when
   * done (or on failure), rolling back any pending changes on error so no partial binding state
   * (new bundle with old head, or vice versa) is left behind.
   */
  public static void rebuildNotebook(Connection connection, int notebookId, Instant rebuildTime)
      throws SQLException {
    boolean originalAutoCommit = connection.getAutoCommit();
    connection.setAutoCommit(false);
    try {
      replaceBinding(connection, notebookId, rebuildTime);
      connection.commit();
    } catch (SQLException | RuntimeException e) {
      connection.rollback();
      throw e;
    } finally {
      connection.setAutoCommit(originalAutoCommit);
    }
  }

  static void replaceBinding(Connection connection, int notebookId, Instant rebuildTime)
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
            rebuildTime)) {
      NotebookGitBundleWriter.BundleWriteResult written =
          NotebookGitBundleWriter.write(gitRepository);
      updateBinding(connection, notebookId, written, rebuildTime);
    }
  }

  private static void updateBinding(
      Connection connection,
      int notebookId,
      NotebookGitBundleWriter.BundleWriteResult written,
      Instant rebuildTime)
      throws SQLException {
    Timestamp rebuildTimestamp = Timestamp.from(rebuildTime);
    try (PreparedStatement statement = connection.prepareStatement(UPDATE_BINDING)) {
      statement.setString(1, written.headObjectId());
      statement.setBytes(2, written.bundleBytes());
      statement.setTimestamp(3, rebuildTimestamp);
      statement.setInt(4, notebookId);
      int updated = statement.executeUpdate();
      if (updated == 0) {
        throw new SQLException("No notebook_git_binding row found for notebook_id=" + notebookId);
      }
    }
  }
}
