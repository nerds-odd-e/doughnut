package db.migration;

import com.odde.doughnut.entities.UngradedNewLastRecallBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000276__backfill_ungraded_new_last_recall extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    UngradedNewLastRecallBackfill.run(context.getConnection());
  }
}
