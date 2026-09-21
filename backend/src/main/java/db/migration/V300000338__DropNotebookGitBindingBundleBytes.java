package db.migration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Contract step for retiring the legacy bundle column: accepted history lives only in {@code
 * notebook_git_accepted_object}, so {@code notebook_git_binding.bundle_bytes} is dropped - but only
 * after {@link NotebookGitAcceptedHistoryCompleteness} confirms every binding's accepted history is
 * complete in native storage. An incomplete history fails this migration before any DDL, leaving
 * the column in place.
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
