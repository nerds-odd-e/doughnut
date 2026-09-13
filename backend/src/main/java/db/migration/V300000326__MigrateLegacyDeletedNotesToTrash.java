package db.migration;

import com.odde.donut.services.notebookGit.NoteLegacyTrashMigration;
import java.time.Instant;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Migrates every retained legacy soft-deleted note into location-based web trash (a {@code _trash}
 * folder subtree inside the note's own notebook), clearing the legacy {@code note.deleted_at}
 * marker only after valid placement. Delegates to {@link NoteLegacyTrashMigration#run}, which
 * manages its own transaction, so this migration opts out of Flyway's default single-transaction
 * wrapping.
 */
public class V300000326__MigrateLegacyDeletedNotesToTrash extends BaseJavaMigration {

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(Context context) throws Exception {
    NoteLegacyTrashMigration.run(context.getConnection(), Instant.now());
  }
}
