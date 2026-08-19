package db.migration;

import com.odde.doughnut.entities.RecallLogElapsedHoursBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000281__reconstruct_null_recall_log_elapsed_hours extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    RecallLogElapsedHoursBackfill.run(context.getConnection());
  }
}
