package db.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000360__DropQuestionNoteId extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws SQLException {
    try (Statement statement = context.getConnection().createStatement()) {
      try (ResultSet rows =
          statement.executeQuery(
              "SELECT COUNT(*) FROM mcq q WHERE NOT EXISTS "
                  + "(SELECT 1 FROM recall_prompt p WHERE p.mcq_id = q.id)")) {
        rows.next();
        if (rows.getLong(1) > 0) {
          throw new IllegalStateException(
              "Question note retirement requires unreferenced question cleanup");
        }
      }
      statement.execute("ALTER TABLE mcq DROP INDEX fk_mcq_note, DROP COLUMN note_id");
    }
  }
}
