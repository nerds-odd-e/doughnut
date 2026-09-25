package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Every notebook stores its files as Git LFS, so code no longer records a representation: new
 * bindings get {@code LFS} from the column default. Refuses before any DDL, naming each notebook,
 * while a binding is still {@code RAW}. The column stays for the old instance of a rolling deploy.
 */
public class V300000342__DefaultNotebookGitBindingAttachmentRepresentationToLfs
    extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws SQLException {
    Connection connection = context.getConnection();
    List<String> rawNotebookIds = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows =
            statement.executeQuery(
                "SELECT notebook_id FROM notebook_git_binding"
                    + " WHERE attachment_representation = 'RAW' ORDER BY notebook_id")) {
      while (rows.next()) {
        rawNotebookIds.add(rows.getString(1));
      }
    }
    if (!rawNotebookIds.isEmpty()) {
      throw new IllegalStateException(
          "Notebooks still store files as RAW; convert them to LFS first: notebooks "
              + String.join(", ", rawNotebookIds));
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "ALTER TABLE `notebook_git_binding`"
              + " ALTER COLUMN `attachment_representation` SET DEFAULT 'LFS'");
    }
  }
}
