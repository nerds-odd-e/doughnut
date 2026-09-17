package db.migration;

import com.odde.donut.services.notebookGit.NotebookGitBaselineRebuild;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Replaces the accepted Git baseline for every live notebook that already has a {@code
 * notebook_git_binding} row with one fresh parentless root commit built from the notebook's current
 * database content, permanently abandoning the notebook's prior accepted history (its original root
 * and every later accepted commit) without a backup or ancestry bridge. Each notebook rebuild
 * manages its own transaction via {@link NotebookGitBaselineRebuild#rebuildNotebook}, so this
 * migration opts out of Flyway's default single-transaction wrapping. A failure on one notebook
 * fails the migration fast; Flyway retries the whole run on the next startup, and an already
 * rebaselined binding is simply replaced again idempotently.
 */
public class V300000330__RebaselineExistingNotebookGitBindings extends BaseJavaMigration {

  private static final String LIVE_BOUND_NOTEBOOKS_QUERY =
      """
      SELECT n.id
      FROM notebook n
      JOIN notebook_git_binding b ON b.notebook_id = n.id
      WHERE n.deleted_at IS NULL
      ORDER BY n.id ASC
      """;

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (PreparedStatement statement = connection.prepareStatement(LIVE_BOUND_NOTEBOOKS_QUERY);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        int notebookId = resultSet.getInt("id");
        NotebookGitBaselineRebuild.rebuildNotebook(connection, notebookId, Instant.now());
      }
    }
  }
}
