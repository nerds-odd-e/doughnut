package db.migration;

import com.odde.doughnut.services.NotePropertyTrackingBackfill;
import java.sql.Timestamp;
import java.time.Instant;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V300000206__seed_note_property_index_and_skipped_property_trackers
    extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Timestamp now = Timestamp.from(Instant.now());
    NotePropertyTrackingBackfill.run(context.getConnection(), now);
  }
}
