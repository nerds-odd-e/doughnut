package db.migration;

import com.odde.doughnut.services.NotePropertyIndexTargetNoteResolutionBackfill;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000220__resolve_note_property_index_target_note_from_notes
    extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    NotePropertyIndexTargetNoteResolutionBackfill.run(context.getConnection());
  }
}
