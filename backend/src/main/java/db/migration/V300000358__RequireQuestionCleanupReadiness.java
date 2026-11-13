package db.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000358__RequireQuestionCleanupReadiness extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws SQLException {
    String gate = context.getConfiguration().getPlaceholders().get("questioncleanup");
    try (Statement statement = context.getConnection().createStatement();
        ResultSet rows =
            statement.executeQuery(
                "SELECT COUNT(*) FROM mcq q WHERE NOT EXISTS "
                    + "(SELECT 1 FROM recall_prompt p WHERE p.mcq_id = q.id) AND NOT ("
                    + gate
                    + ")")) {
      rows.next();
      if (rows.getLong(1) > 0) {
        throw new IllegalStateException(
            "Unreferenced question cleanup requires deliberate enablement before upgrading");
      }
    }
  }
}
