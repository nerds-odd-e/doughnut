package db.migration;

import com.odde.donut.services.NoteLevelIndexBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000311__BackfillNoteLevelFromLegacyColumn extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    NoteLevelIndexBackfill.run(context.getConnection());
  }
}
