package db.migration;

import com.odde.donut.services.notebookGit.NotebookGitFleetCutoverBackfill;
import java.time.Instant;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Fleet cutover: gives every live pre-Git notebook its accepted Git binding in one migration run.
 * Opts out of Flyway's default single-transaction wrapping so each notebook can be committed
 * independently (see {@link NotebookGitFleetCutoverBackfill}), keeping a retry after a partial
 * failure safe and duplicate-free.
 */
public class V300000320__CutoverExistingNotebooksToGit extends BaseJavaMigration {

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(Context context) throws Exception {
    NotebookGitFleetCutoverBackfill.run(context.getConnection(), Instant.now());
  }
}
