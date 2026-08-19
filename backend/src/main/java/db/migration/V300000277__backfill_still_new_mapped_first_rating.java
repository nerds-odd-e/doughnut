package db.migration;

import com.odde.doughnut.entities.StillNewMappedFirstRatingBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000277__backfill_still_new_mapped_first_rating extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    StillNewMappedFirstRatingBackfill.run(context.getConnection());
  }
}
