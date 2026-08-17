package db.migration;

import com.odde.doughnut.services.NoteConceptTypeBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000269__backfill_note_concept_type extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    NoteConceptTypeBackfill.run(
        context.getConnection(),
        context.getConfiguration().getPlaceholders().get("note_concept_type_backfill"));
  }
}
