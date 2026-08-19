package db.migration;

import com.odde.doughnut.entities.RecallLogDsrBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000283__rebuild_dsr_from_recall_log extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    RecallLogDsrBackfill.run(context.getConnection());
  }
}
