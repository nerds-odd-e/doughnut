package db.migration;

import com.odde.doughnut.entities.OverCapStabilityBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000274__backfill_over_cap_stability extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    OverCapStabilityBackfill.run(context.getConnection());
  }
}
