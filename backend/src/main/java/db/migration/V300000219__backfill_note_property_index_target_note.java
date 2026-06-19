package db.migration;

import com.odde.doughnut.services.NotePropertyIndexTargetNoteBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000219__backfill_note_property_index_target_note extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    NotePropertyIndexTargetNoteBackfill.run(context.getConnection());
  }
}
