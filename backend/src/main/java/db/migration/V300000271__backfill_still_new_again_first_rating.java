package db.migration;

import com.odde.doughnut.entities.StillNewAgainFirstRatingBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000271__backfill_still_new_again_first_rating extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    StillNewAgainFirstRatingBackfill.run(
        context.getConnection(),
        context.getConfiguration().getPlaceholders().get("still_new_again_first_rating_backfill"));
  }
}
