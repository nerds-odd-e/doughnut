package db.migration;

import com.odde.doughnut.entities.StillNewFirstRatingBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000272__backfill_still_new_hard_first_rating extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    StillNewFirstRatingBackfill.runHard(
        context.getConnection(),
        context.getConfiguration().getPlaceholders().get("still_new_hard_first_rating_backfill"));
  }
}
