package db.migration;

import com.odde.doughnut.services.DisplayNameOsInvalidCharsBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000280__convert_os_invalid_display_names extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    DisplayNameOsInvalidCharsBackfill.run(context.getConnection());
  }
}
