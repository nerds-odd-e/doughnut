package db.migration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * One-time upgrade step that drops {@code notebook_git_binding.bundle_bytes}: accepted history
 * lives only in {@code notebook_git_accepted_object}, so the column is dropped - but only after
 * {@link NotebookGitAcceptedHistoryCompleteness} confirms every binding's accepted history is
 * complete in native storage. An incomplete history fails this migration before any DDL, leaving
 * the column in place. A fresh install runs it over an empty binding table, so the check passes
 * trivially.
 */
public class V300000338__DropNotebookGitBindingBundleBytes extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws SQLException, IOException {
    Connection connection = context.getConnection();
    NotebookGitAcceptedHistoryCompleteness.requireEveryAcceptedHistoryComplete(connection);
    try (Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE `notebook_git_binding` DROP COLUMN `bundle_bytes`");
    }
  }
}
