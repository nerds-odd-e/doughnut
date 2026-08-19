package db.migration;

import com.odde.doughnut.entities.AliasRecallLogGradeBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000279__rewrite_alias_recall_log_grades_to_hard_and_again
    extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    AliasRecallLogGradeBackfill.run(context.getConnection());
  }
}
