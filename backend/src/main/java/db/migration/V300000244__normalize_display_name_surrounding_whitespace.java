package db.migration;

import com.odde.doughnut.services.DisplayNameSurroundingWhitespaceBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000244__normalize_display_name_surrounding_whitespace extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    DisplayNameSurroundingWhitespaceBackfill.run(context.getConnection());
  }
}
