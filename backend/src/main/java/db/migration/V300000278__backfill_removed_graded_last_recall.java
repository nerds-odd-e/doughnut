package db.migration;

import com.odde.doughnut.entities.RemovedGradedLastRecallBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000278__backfill_removed_graded_last_recall extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    RemovedGradedLastRecallBackfill.run(context.getConnection());
  }
}
