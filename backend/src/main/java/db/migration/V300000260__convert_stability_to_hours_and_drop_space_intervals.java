package db.migration;

import com.odde.doughnut.services.StabilityIndexToHoursBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000260__convert_stability_to_hours_and_drop_space_intervals
    extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    StabilityIndexToHoursBackfill.run(context.getConnection());
  }
}
