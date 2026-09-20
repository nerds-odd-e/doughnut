package db.migration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * One-time backfill: converts every {@code notebook_git_binding} row that predates native Git
 * object storage (slices 3-5) into the native {@code notebook_git_accepted_object} store, so an
 * existing binding never opened since upgrade retains its exact head/history without depending on
 * {@link com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore}'s lazy
 * convert-on-first-open path. {@code bundle_bytes} and {@code accepted_git_object_id} are left
 * completely untouched; this migration only adds native rows. See {@link
 * NotebookGitAcceptedObjectBackfill} for the actual algorithm, kept as a plain, independently
 * testable class since this migration itself runs automatically, once per environment, outside
 * Spring context.
 *
 * <p>Each binding is converted and committed as its own unit of work ({@link
 * #canExecuteInTransaction()} is {@code false}), so a restart after an interrupted run resumes
 * safely: an already-converted binding no longer matches the "still empty native store" query and
 * is skipped.
 */
public class V300000335__BackfillNotebookGitAcceptedObjects extends BaseJavaMigration {

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
