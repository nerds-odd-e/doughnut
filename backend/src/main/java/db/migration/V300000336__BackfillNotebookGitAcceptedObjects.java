package db.migration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * One-time upgrade step: converts every {@code notebook_git_binding} row that stored its accepted
 * history only as {@code bundle_bytes} into the native {@code notebook_git_accepted_object} store,
 * keeping its exact head and history. {@code bundle_bytes} and {@code accepted_git_object_id} are
 * left untouched; this migration only adds native rows. A fresh install runs it over empty tables,
 * where it does nothing. See {@link NotebookGitAcceptedObjectBackfill} for the algorithm.
 *
 * <p>Each binding is converted and committed as its own unit of work ({@link
 * #canExecuteInTransaction()} is {@code false}), so a restart after an interrupted run resumes
 * safely: an already-converted binding no longer matches the "still empty native store" query and
 * is skipped.
 */
public class V300000336__BackfillNotebookGitAcceptedObjects extends BaseJavaMigration {

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(Context context) throws SQLException, IOException {
    Connection connection = context.getConnection();
    NotebookGitAcceptedObjectBackfill.backfillLegacyBindings(connection);
  }
}
