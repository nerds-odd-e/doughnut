package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.currentPortableTreeFromDb;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.readTreeEntries;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.insertBinding;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.insertFolder;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.insertNote;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.insertNotebook;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.insertUser;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.legacyDeletedAt;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.nullableInt;
import static com.odde.donut.services.notebookGit.PreUpgradeFixtureRows.seedTimestamp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Note;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Scale rehearsal of the actual registered Flyway upgrade (V300000326-V300000328) against one large
 * populated pre-326 fixture (10,000 legacy soft-deleted notes plus active content, in one notebook)
 * seeded via raw JDBC into {@link PreUpgradeFixtureSchema}, alongside smaller neighboring notebooks
 * — reusing {@link NotebookUpgradeDataPreservationTest}'s seed/proof shape (slice 3) at scale, per
 * this story's plan: "Reuse small cases for error permutations instead of repeating the full matrix
 * at scale." The exact-title collision/suffix matrix itself was already proved at small scale by
 * slice 3 and {@link NoteLegacyTrashMigrationInterruptionTest} (slice 4); this class proves the
 * same mechanics do not degrade, duplicate, or corrupt data when the candidate set is thousands of
 * rows large, and that the actual chain (and one interrupted retry) completes in that regime.
 *
 * <p>Fixture shape for the large notebook ("Bulk Notebook"):
 *
 * <ul>
 *   <li>10 sibling folders ({@code Docs/Bucket0..9}) sharing the nested parent {@code Docs} — 1,000
 *       legacy soft-deleted notes per bucket (10,000 total), exercising {@code
 *       NoteLegacyTrashMigration}'s folder-trail/child-folder lookups at volume with shared nested
 *       parents.
 *   <li>An existing {@code _trash} subtree pre-seeded before the upgrade (not just legacy
 *       soft-deleted notes): {@code _trash}, {@code _trash/Docs}, and {@code
 *       _trash/Docs/Bucket{0,3,5,6,9}} already exist, with real occupant notes at several bucket
 *       destinations, forcing the migration to create only the remaining five bucket-level trash
 *       folders ({@code Bucket1,2,4,7,8}) rather than any duplicate.
 *   <li>Bounded occupied-name collisions: seven legacy notes land on a destination title already
 *       occupied by a pre-existing trash note, forcing {@code NoteLegacyTrashMigration}'s
 *       collision-avoidance suffix ({@code " (2)"}).
 *   <li>Long-title suffixing: one legacy note has a title at {@link Note#MAX_TITLE_LENGTH} (150)
 *       characters and collides with an identically-titled pre-existing trash occupant, forcing
 *       {@code availableTitleAt}'s truncate-then-suffix branch (title cut to 146 chars + {@code "
 *       (2)"} = 150).
 *   <li>One legacy note already parked directly under an existing {@code _trash} bucket folder
 *       before the upgrade — only {@code deleted_at} clears, no relocation.
 *   <li>Representative linked learning/reference rows: a sample of six bulk notes each carry one
 *       {@code memory_tracker} (recall history) and one {@code authored_note_reference} (authored
 *       wiki link) row, proving those FK-linked rows survive a note's relocation untouched.
 *   <li>Two smaller neighboring notebooks (one live bound, one bound-but-soft-deleted) alongside
 *       the large one, mirroring slice 3's bound/soft-deleted split, so the fleet migration (327)
 *       and the whole-schema conversion (326) are proved processing a mixed-size fleet, not only
 *       the one giant notebook.
 * </ul>
 *
 * <p>Every one of the 10,002 seeded legacy-deleted notes (10,000 in the bulk notebook, one per
 * neighboring notebook) is compared by identity/content before and after — not just counted — and
 * every non-deleted note, every {@code memory_tracker}, and every {@code authored_note_reference}
 * row is asserted byte-for-byte unchanged. The bound notebooks' complete Portable trees are
 * verified against the real post-upgrade database content via the same reusable {@code
 * currentPortableTreeFromDb}/{@code readTreeEntries} comparison slices 3 and 5 already use — never
 * against a hand-typed expected list, which would be impractical at 10,000+ tree entries.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotebookUpgradeAtScaleTest {

  @Autowired DataSource dataSource;

  private static final int BUCKET_COUNT = 10;
  private static final int NOTES_PER_BUCKET = 1000;
  private static final int TOTAL_BULK_NOTES = BUCKET_COUNT * NOTES_PER_BUCKET;
  private static final Set<Integer> PRE_EXISTING_TRASH_BUCKETS = Set.of(0, 3, 5, 6, 9);
  private static final int ALREADY_IN_TRASH_BUCKET = 5;
  private static final int ALREADY_IN_TRASH_INDEX = 500;
  private static final int LONG_TITLE_BUCKET = 9;
  private static final int LONG_TITLE_INDEX = 0;
  private static final String LONG_TITLE = "A".repeat(Note.MAX_TITLE_LENGTH);
  private static final List<int[]> NORMAL_COLLISION_SPECS =
      List.of(
          new int[] {0, 0},
          new int[] {3, 100},
          new int[] {3, 101},
          new int[] {3, 999},
          new int[] {6, 500},
          new int[] {6, 501},
          new int[] {6, 999});
  private static final int EXPECTED_TOUCHED_COUNT = TOTAL_BULK_NOTES + 2;
  private static final String LARGE_GIT_OBJECT_ID = "a".repeat(40);
  private static final String SMALL_LIVE_GIT_OBJECT_ID = "b".repeat(40);
  private static final String SMALL_DELETED_GIT_OBJECT_ID = "c".repeat(40);

  @Test
  void tenThousandLegacyDeletedNotesSurviveTheActualFlyway326To328ChainInOneRun() throws Exception {
    String ownerSchemaName = ownerSchemaName();

    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      Connection connection = fixture.connection();
      FixtureIds ids = seedFixture(connection);
      List<NoteSnapshot> before = readAllNotes(connection);
      List<MemoryTrackerRow> trackersBefore = readMemoryTrackers(connection);
      List<AuthoredReferenceRow> referencesBefore = readAuthoredReferences(connection);

      System.out.println("[scale] MySQL version: " + mysqlVersion(connection));
      logMemory("before migration");

      long start326 = System.nanoTime();
      fixture.flywayConfig().target(MigrationVersion.fromVersion("300000326")).load().migrate();
      long after326 = System.nanoTime();
      fixture.flywayConfig().target(MigrationVersion.fromVersion("300000327")).load().migrate();
      long after327 = System.nanoTime();

      List<NoteSnapshot> afterConversion = readAllNotes(connection);
      assertFullUpgradeCorrectness(
          connection, before, afterConversion, ids, trackersBefore, referencesBefore, "clean run");

      fixture.flywayConfig().load().migrate();
      long after328 = System.nanoTime();

      System.out.printf(
          "[scale] clean run elapsed ms: 326=%d 327=%d 328=%d total=%d%n",
          millis(start326, after326),
          millis(after326, after327),
          millis(after327, after328),
          millis(start326, after328));
      logMemory("after migration");

      assertSchemaOnlyChangeAfter328(connection, afterConversion);
    }
  }

  @Test
  void retryingAnInterruptedUpgradeAtScaleCompletesExactlyOnce() throws Exception {
    String ownerSchemaName = ownerSchemaName();

    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      Connection connection = fixture.connection();
      FixtureIds ids = seedFixture(connection);
      List<NoteSnapshot> before = readAllNotes(connection);
      List<MemoryTrackerRow> trackersBefore = readMemoryTrackers(connection);
      List<AuthoredReferenceRow> referencesBefore = readAuthoredReferences(connection);

      // Interruption point (reusing slice 4/5's technique, one representative point at scale, per
      // this story's plan): the real V300000326 conversion entry point is invoked directly, outside
      // Flyway, against a connection discarded immediately afterward — so the whole 10,000+ note
      // conversion genuinely commits for real, but flyway_schema_history has no row for 300000326
      // and 327/328 have not run at all. Mirrors a process loss after commit but before Flyway's
      // bookkeeping, at scale instead of the handful of notes slice 4 used.
      long startDirect = System.nanoTime();
      try (Connection interrupted = fixture.openConnection()) {
        NoteLegacyTrashMigration.run(interrupted, Instant.now());
      }
      long afterDirect = System.nanoTime();
      System.out.printf(
          "[scale] direct (uninstrumented) 326 conversion elapsed ms: %d%n",
          millis(startDirect, afterDirect));

      assertNoHistoryRowFor(connection, "300000326");
      assertThat(
          "the direct conversion left no remaining legacy-deleted candidates",
          candidateCount(connection),
          equalTo(0));

      // Retry through 327 first (deleted_at still exists) so the full identity/collision/tree
      // proof below can run before 328 drops the column, matching the clean run's phasing.
      Flyway flywayTo327 =
          fixture.flywayConfig().target(MigrationVersion.fromVersion("300000327")).load();
      long startRetry = System.nanoTime();
      flywayTo327.repair();
      flywayTo327.migrate();
      long afterRetryTo327 = System.nanoTime();

      assertExactlyOneSuccessfulHistoryRowFor(connection, "300000326");
      assertExactlyOneSuccessfulHistoryRowFor(connection, "300000327");

      List<NoteSnapshot> afterConversion = readAllNotes(connection);
      assertFullUpgradeCorrectness(
          connection,
          before,
          afterConversion,
          ids,
          trackersBefore,
          referencesBefore,
          "interrupted retry");

      fixture.flywayConfig().load().migrate();
      long afterRetryTo328 = System.nanoTime();
      System.out.printf(
          "[scale] repair()+migrate() retry elapsed ms: through327=%d then328=%d total=%d%n",
          millis(startRetry, afterRetryTo327),
          millis(afterRetryTo327, afterRetryTo328),
          millis(startRetry, afterRetryTo328));

      assertExactlyOneSuccessfulHistoryRowFor(connection, "300000328");
      assertSchemaOnlyChangeAfter328(connection, afterConversion);
    }
  }

  private String ownerSchemaName() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      return connection.getCatalog();
    }
  }

  private static long millis(long startNanos, long endNanos) {
    return (endNanos - startNanos) / 1_000_000;
  }

  private static void logMemory(String label) {
    Runtime runtime = Runtime.getRuntime();
    long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    long maxMb = runtime.maxMemory() / (1024 * 1024);
    System.out.printf("[scale] JVM heap %s: used=%dMB max=%dMB%n", label, usedMb, maxMb);
  }

  private String mysqlVersion(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT VERSION()")) {
      rs.next();
      return rs.getString(1);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Fixture seeding
  // ---------------------------------------------------------------------------------------------

  private record FixtureIds(
      int notebookLarge,
      Map<Integer, Integer> bucketFolderIds,
      Map<Integer, Integer> trashBucketFolderIds,
      Map<String, Integer> specialNoteIds,
      int notebookSmallLive,
      int notebookSmallDeleted) {}

  private FixtureIds seedFixture(Connection connection) throws SQLException {
    Timestamp seedTs = seedTimestamp();
    Timestamp legacyTs = legacyDeletedAt();

    int ownerLarge = insertUser(connection, "Owner Large", "owner-large");
    int notebookLarge = insertNotebook(connection, ownerLarge, "Bulk Notebook", false);

    int docs = insertFolder(connection, notebookLarge, null, "Docs", "# Docs readme");
    Map<Integer, Integer> bucketFolderIds = new LinkedHashMap<>();
    for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
      bucketFolderIds.put(
          bucket, insertFolder(connection, notebookLarge, docs, "Bucket" + bucket, null));
    }

    int trashRoot = insertFolder(connection, notebookLarge, null, "_trash", null);
    int trashDocs = insertFolder(connection, notebookLarge, trashRoot, "Docs", null);
    Map<Integer, Integer> trashBucketFolderIds = new LinkedHashMap<>();
    for (int bucket : PRE_EXISTING_TRASH_BUCKETS) {
      trashBucketFolderIds.put(
          bucket, insertFolder(connection, notebookLarge, trashDocs, "Bucket" + bucket, null));
    }

    // Pre-existing trash occupants at the exact destinations seven ordinary-length legacy notes and
    // one long-title legacy note will convert to, forcing collision-avoidance suffixing.
    for (int[] spec : NORMAL_COLLISION_SPECS) {
      insertNote(
          connection,
          notebookLarge,
          trashBucketFolderIds.get(spec[0]),
          bulkNoteTitle(spec[0], spec[1]),
          "Existing trash occupant",
          null);
    }
    insertNote(
        connection,
        notebookLarge,
        trashBucketFolderIds.get(LONG_TITLE_BUCKET),
        LONG_TITLE,
        "Existing trash occupant (long title)",
        null);

    bulkInsertNotes(
        connection, notebookLarge, bucketFolderIds, trashBucketFolderIds, seedTs, legacyTs);

    // A handful of active (never-deleted) notes directly under Docs, proving active content in the
    // same notebook as the 10,000 legacy notes survives the upgrade untouched.
    for (int i = 0; i < 5; i++) {
      insertNote(connection, notebookLarge, docs, "Active Note " + i, "Active content " + i, null);
    }

    // Representative linked learning/reference rows on a bounded sample of bulk notes.
    int[][] sampleSpecs = {{0, 1}, {0, 501}, {3, 1}, {3, 501}, {6, 1}, {6, 501}};
    for (int[] spec : sampleSpecs) {
      int folderId = bucketFolderIds.get(spec[0]);
      String title = bulkNoteTitle(spec[0], spec[1]);
      insertMemoryTrackerForNoteAt(connection, ownerLarge, folderId, title);
      insertAuthoredReferenceForNoteAt(connection, folderId, title);
    }

    insertBinding(connection, notebookLarge, LARGE_GIT_OBJECT_ID);

    // Capture the special-case note ids now (before their folder_id changes) so post-migration
    // assertions can look them up by id rather than by their now-obsolete original folder/title.
    Map<String, Integer> specialNoteIds = new LinkedHashMap<>();
    for (int[] spec : NORMAL_COLLISION_SPECS) {
      specialNoteIds.put(
          "collision-" + spec[0] + "-" + spec[1],
          noteIdByFolderAndTitle(
              connection, bucketFolderIds.get(spec[0]), bulkNoteTitle(spec[0], spec[1])));
    }
    specialNoteIds.put(
        "long-title",
        noteIdByFolderAndTitle(connection, bucketFolderIds.get(LONG_TITLE_BUCKET), LONG_TITLE));
    specialNoteIds.put(
        "already-in-trash",
        noteIdByFolderAndTitle(
            connection,
            trashBucketFolderIds.get(ALREADY_IN_TRASH_BUCKET),
            bulkNoteTitle(ALREADY_IN_TRASH_BUCKET, ALREADY_IN_TRASH_INDEX)));

    int notebookSmallLive = seedSmallLiveNotebook(connection, "Small Live");
    int notebookSmallDeleted = seedSmallDeletedNotebook(connection, "Small Deleted");

    return new FixtureIds(
        notebookLarge,
        bucketFolderIds,
        trashBucketFolderIds,
        specialNoteIds,
        notebookSmallLive,
        notebookSmallDeleted);
  }

  private static String bulkNoteTitle(int bucket, int n) {
    return "Bulk Note " + bucket + "-" + n;
  }

  private void bulkInsertNotes(
      Connection connection,
      int notebookId,
      Map<Integer, Integer> bucketFolderIds,
      Map<Integer, Integer> trashBucketFolderIds,
      Timestamp seedTs,
      Timestamp legacyTs)
      throws SQLException {
    String insertPrefix =
        "INSERT INTO note (notebook_id, folder_id, title, content, deleted_at, created_at,"
            + " updated_at) VALUES ";
    StringBuilder sql = new StringBuilder(insertPrefix);
    int rowsInBatch = 0;
    try (Statement statement = connection.createStatement()) {
      for (int bucket = 0; bucket < BUCKET_COUNT; bucket++) {
        for (int n = 0; n < NOTES_PER_BUCKET; n++) {
          String title = bulkNoteTitle(bucket, n);
          int folderId = bucketFolderIds.get(bucket);
          if (bucket == ALREADY_IN_TRASH_BUCKET && n == ALREADY_IN_TRASH_INDEX) {
            // Already parked directly under the existing trash bucket folder before the upgrade.
            folderId = trashBucketFolderIds.get(bucket);
          }
          if (bucket == LONG_TITLE_BUCKET && n == LONG_TITLE_INDEX) {
            title = LONG_TITLE;
          }
          if (rowsInBatch > 0) {
            sql.append(',');
          }
          sql.append('(')
              .append(notebookId)
              .append(',')
              .append(folderId)
              .append(",'")
              .append(title)
              .append("','Content ")
              .append(bucket)
              .append('-')
              .append(n)
              .append("','")
              .append(legacyTs)
              .append("','")
              .append(seedTs)
              .append("','")
              .append(seedTs)
              .append("')");
          rowsInBatch++;
          if (rowsInBatch == 500) {
            statement.execute(sql.toString());
            sql.setLength(0);
            sql.append(insertPrefix);
            rowsInBatch = 0;
          }
        }
      }
      if (rowsInBatch > 0) {
        statement.execute(sql.toString());
      }
    }
  }

  private void insertMemoryTrackerForNoteAt(
      Connection connection, int userId, int folderId, String title) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT INTO memory_tracker (user_id, note_id, type, property_key, stability,"
                + " difficulty, next_recall_at, last_recalled_at, assimilated_at,"
                + " removed_from_tracking) SELECT ?, id, 'UNDERSTANDING', '', 1.5, 0.6,"
                + " '2026-01-01 00:00:00', '2025-12-01 00:00:00', '2025-11-01 00:00:00', 0 FROM"
                + " note WHERE folder_id = ? AND title = ?")) {
      statement.setInt(1, userId);
      statement.setInt(2, folderId);
      statement.setString(3, title);
      int updated = statement.executeUpdate();
      if (updated != 1) {
        throw new IllegalStateException("Expected exactly one note at " + folderId + "/" + title);
      }
    }
  }

  private void insertAuthoredReferenceForNoteAt(Connection connection, int folderId, String title)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT INTO authored_note_reference (source_note_id, kind, authored_link,"
                + " display_text, document_order, wiki_note_portion) SELECT id,"
                + " 'WIKI_PORTABLE_PATH', ?, ?, 0, ? FROM note WHERE folder_id = ? AND title ="
                + " ?")) {
      statement.setString(1, "Docs/" + title);
      statement.setString(2, title);
      statement.setString(3, "Docs/" + title);
      statement.setInt(4, folderId);
      statement.setString(5, title);
      int updated = statement.executeUpdate();
      if (updated != 1) {
        throw new IllegalStateException("Expected exactly one note at " + folderId + "/" + title);
      }
    }
  }

  private int seedSmallLiveNotebook(Connection connection, String label) throws SQLException {
    int owner = insertUser(connection, "Owner " + label, "owner-" + label.replace(' ', '-'));
    int notebookId = insertNotebook(connection, owner, label + " Notebook", false);
    insertNote(connection, notebookId, null, "Kept " + label, "Active content " + label, null);
    insertNote(
        connection, notebookId, null, "Gone " + label, "Gone content " + label, legacyDeletedAt());
    insertBinding(connection, notebookId, SMALL_LIVE_GIT_OBJECT_ID);
    return notebookId;
  }

  private int seedSmallDeletedNotebook(Connection connection, String label) throws SQLException {
    int owner = insertUser(connection, "Owner " + label, "owner-" + label.replace(' ', '-'));
    int notebookId = insertNotebook(connection, owner, label + " Notebook", true);
    insertNote(connection, notebookId, null, "Kept " + label, "Active content " + label, null);
    insertNote(
        connection, notebookId, null, "Gone " + label, "Gone content " + label, legacyDeletedAt());
    insertBinding(connection, notebookId, SMALL_DELETED_GIT_OBJECT_ID);
    return notebookId;
  }

  private int noteIdByFolderAndTitle(Connection connection, int folderId, String title)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("SELECT id FROM note WHERE folder_id = ? AND title = ?")) {
      statement.setInt(1, folderId);
      statement.setString(2, title);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          throw new IllegalStateException("No note at folder " + folderId + " title " + title);
        }
        return rs.getInt("id");
      }
    }
  }

  private int candidateCount(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT COUNT(*) AS c FROM note WHERE deleted_at IS NOT NULL AND notebook_id IS"
                    + " NOT NULL")) {
      rs.next();
      return rs.getInt("c");
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Row capture
  // ---------------------------------------------------------------------------------------------

  private record NoteSnapshot(
      int id,
      int notebookId,
      Integer folderId,
      String title,
      String content,
      Timestamp deletedAt,
      Timestamp createdAt,
      Timestamp updatedAt) {
    NoteSnapshotFinal toFinal() {
      return new NoteSnapshotFinal(id, notebookId, folderId, title, content, createdAt, updatedAt);
    }
  }

  private record NoteSnapshotFinal(
      int id,
      int notebookId,
      Integer folderId,
      String title,
      String content,
      Timestamp createdAt,
      Timestamp updatedAt) {}

  private record FolderNode(Integer parentFolderId, String name) {}

  private record MemoryTrackerRow(
      int id,
      int userId,
      int noteId,
      String type,
      String propertyKey,
      float stability,
      Float difficulty,
      boolean removedFromTracking) {}

  private record AuthoredReferenceRow(
      int id, int sourceNoteId, String kind, String authoredLink, String displayText) {}

  private List<NoteSnapshot> readAllNotes(Connection connection) throws SQLException {
    List<NoteSnapshot> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, notebook_id, folder_id, title, content, deleted_at, created_at,"
                    + " updated_at FROM note ORDER BY id ASC")) {
      while (rs.next()) {
        rows.add(
            new NoteSnapshot(
                rs.getInt("id"),
                rs.getInt("notebook_id"),
                nullableInt(rs, "folder_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getTimestamp("deleted_at"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")));
      }
    }
    return rows;
  }

  private List<NoteSnapshotFinal> readAllNotesFinal(Connection connection) throws SQLException {
    List<NoteSnapshotFinal> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, notebook_id, folder_id, title, content, created_at, updated_at FROM"
                    + " note ORDER BY id ASC")) {
      while (rs.next()) {
        rows.add(
            new NoteSnapshotFinal(
                rs.getInt("id"),
                rs.getInt("notebook_id"),
                nullableInt(rs, "folder_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")));
      }
    }
    return rows;
  }

  private Map<Integer, FolderNode> readFolderAncestry(Connection connection) throws SQLException {
    Map<Integer, FolderNode> folders = new LinkedHashMap<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, parent_folder_id, name FROM folder ORDER BY id ASC")) {
      while (rs.next()) {
        folders.put(
            rs.getInt("id"),
            new FolderNode(nullableInt(rs, "parent_folder_id"), rs.getString("name")));
      }
    }
    return folders;
  }

  private List<MemoryTrackerRow> readMemoryTrackers(Connection connection) throws SQLException {
    List<MemoryTrackerRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, user_id, note_id, type, property_key, stability, difficulty,"
                    + " removed_from_tracking FROM memory_tracker ORDER BY id ASC")) {
      while (rs.next()) {
        float difficultyValue = rs.getFloat("difficulty");
        rows.add(
            new MemoryTrackerRow(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("note_id"),
                rs.getString("type"),
                rs.getString("property_key"),
                rs.getFloat("stability"),
                rs.wasNull() ? null : difficultyValue,
                rs.getBoolean("removed_from_tracking")));
      }
    }
    return rows;
  }

  private List<AuthoredReferenceRow> readAuthoredReferences(Connection connection)
      throws SQLException {
    List<AuthoredReferenceRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, source_note_id, kind, authored_link, display_text FROM"
                    + " authored_note_reference ORDER BY id ASC")) {
      while (rs.next()) {
        rows.add(
            new AuthoredReferenceRow(
                rs.getInt("id"),
                rs.getInt("source_note_id"),
                rs.getString("kind"),
                rs.getString("authored_link"),
                rs.getString("display_text")));
      }
    }
    return rows;
  }

  // ---------------------------------------------------------------------------------------------
  // Assertions
  // ---------------------------------------------------------------------------------------------

  private void assertFullUpgradeCorrectness(
      Connection connection,
      List<NoteSnapshot> before,
      List<NoteSnapshot> after,
      FixtureIds ids,
      List<MemoryTrackerRow> trackersBefore,
      List<AuthoredReferenceRow> referencesBefore,
      String scenarioLabel)
      throws Exception {
    Map<Integer, FolderNode> folderAncestry = readFolderAncestry(connection);

    assertAllNotesPreserved(before, after, folderAncestry, scenarioLabel);
    assertNoDuplicateTitlesPerFolder(connection);

    Map<Integer, NoteSnapshot> afterById =
        after.stream().collect(Collectors.toMap(NoteSnapshot::id, r -> r));
    assertEngineeredCollisionsResolvedExactlyOnce(afterById, ids);

    assertThat(
        scenarioLabel + ": memory_tracker rows untouched",
        readMemoryTrackers(connection),
        equalTo(trackersBefore));
    assertThat(
        scenarioLabel + ": authored_note_reference rows untouched",
        readAuthoredReferences(connection),
        equalTo(referencesBefore));

    assertBindingReplaced(connection, ids.notebookLarge());
    assertBindingReplaced(connection, ids.notebookSmallLive());
    assertBindingUntouched(connection, ids.notebookSmallDeleted(), SMALL_DELETED_GIT_OBJECT_ID);

    assertNotebookTreeMatchesDbGenerically(connection, ids.notebookLarge());
    assertNotebookTreeMatchesDbGenerically(connection, ids.notebookSmallLive());
  }

  private void assertSchemaOnlyChangeAfter328(
      Connection connection, List<NoteSnapshot> afterConversion) throws SQLException {
    List<NoteSnapshotFinal> expected = afterConversion.stream().map(NoteSnapshot::toFinal).toList();
    List<NoteSnapshotFinal> actual = readAllNotesFinal(connection);
    assertThat(
        "V300000328 changes nothing but the schema: every row equals its post-conversion state",
        actual,
        equalTo(expected));
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SHOW COLUMNS FROM note LIKE 'deleted_at'")) {
      assertThat("note.deleted_at must be gone after V300000328", rs.next(), is(false));
    }
  }

  private void assertAllNotesPreserved(
      List<NoteSnapshot> before,
      List<NoteSnapshot> after,
      Map<Integer, FolderNode> folderAncestry,
      String scenarioLabel) {
    Map<Integer, NoteSnapshot> afterById =
        after.stream().collect(Collectors.toMap(NoteSnapshot::id, r -> r));
    assertThat(
        scenarioLabel + ": no note rows added or removed by the upgrade",
        after.size(),
        equalTo(before.size()));

    int touchedCount = 0;
    for (NoteSnapshot b : before) {
      NoteSnapshot a = afterById.get(b.id());
      assertThat(scenarioLabel + ": note " + b.id() + " must still exist", a, is(notNullValue()));
      assertThat(
          scenarioLabel + ": note " + b.id() + " notebook unchanged",
          a.notebookId(),
          equalTo(b.notebookId()));
      assertThat(
          scenarioLabel + ": note " + b.id() + " content unchanged",
          a.content(),
          equalTo(b.content()));
      assertThat(
          scenarioLabel + ": note " + b.id() + " created_at unchanged",
          a.createdAt(),
          equalTo(b.createdAt()));

      if (b.deletedAt() != null) {
        touchedCount++;
        assertThat(
            scenarioLabel + ": legacy note " + b.id() + " deleted_at cleared",
            a.deletedAt(),
            is(nullValue()));
        assertThat(
            scenarioLabel + ": legacy note " + b.id() + " updated_at bumped",
            a.updatedAt(),
            is(not(equalTo(b.updatedAt()))));
        assertThat(
            scenarioLabel + ": legacy note " + b.id() + " title within max length",
            a.title().length(),
            lessThanOrEqualTo(Note.MAX_TITLE_LENGTH));
        assertThat(
            scenarioLabel + ": legacy note " + b.id() + " resolves under _trash",
            isUnderTrash(folderAncestry, a.folderId()),
            is(true));
      } else {
        assertThat(
            scenarioLabel + ": untouched note " + b.id() + " deleted_at unchanged",
            a.deletedAt(),
            equalTo(b.deletedAt()));
        assertThat(
            scenarioLabel + ": untouched note " + b.id() + " title unchanged",
            a.title(),
            equalTo(b.title()));
        assertThat(
            scenarioLabel + ": untouched note " + b.id() + " folder unchanged",
            a.folderId(),
            equalTo(b.folderId()));
        assertThat(
            scenarioLabel + ": untouched note " + b.id() + " updated_at unchanged",
            a.updatedAt(),
            equalTo(b.updatedAt()));
      }
    }

    assertThat(
        scenarioLabel + ": exactly the seeded legacy-deleted notes were converted",
        touchedCount,
        equalTo(EXPECTED_TOUCHED_COUNT));
  }

  private boolean isUnderTrash(Map<Integer, FolderNode> folders, Integer folderId) {
    Integer current = folderId;
    while (current != null) {
      FolderNode node = folders.get(current);
      if (node == null) {
        return false;
      }
      if (node.parentFolderId() == null) {
        return node.name().equalsIgnoreCase(NoteLegacyTrashMigration.TRASH_ROOT_NAME);
      }
      current = node.parentFolderId();
    }
    return false;
  }

  private void assertNoDuplicateTitlesPerFolder(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT folder_id, LOWER(title) AS lt, COUNT(*) AS c FROM note WHERE folder_id IS"
                    + " NOT NULL GROUP BY folder_id, LOWER(title) HAVING COUNT(*) > 1")) {
      assertThat("no duplicate titles within any folder after the upgrade", rs.next(), is(false));
    }
  }

  private void assertEngineeredCollisionsResolvedExactlyOnce(
      Map<Integer, NoteSnapshot> afterById, FixtureIds ids) {
    for (int[] spec : NORMAL_COLLISION_SPECS) {
      NoteSnapshot note =
          afterById.get(ids.specialNoteIds().get("collision-" + spec[0] + "-" + spec[1]));
      assertThat(
          "collision note " + spec[0] + "-" + spec[1] + " suffixed exactly once",
          note.title(),
          equalTo(bulkNoteTitle(spec[0], spec[1]) + " (2)"));
      assertThat(
          "collision note " + spec[0] + "-" + spec[1] + " lands in the pre-existing trash folder",
          note.folderId(),
          equalTo(ids.trashBucketFolderIds().get(spec[0])));
    }

    NoteSnapshot longTitleNote = afterById.get(ids.specialNoteIds().get("long-title"));
    String expectedSuffix = " (2)";
    String expectedLongTitle =
        LONG_TITLE.substring(0, Note.MAX_TITLE_LENGTH - expectedSuffix.length()) + expectedSuffix;
    assertThat(
        "long title truncated then suffixed to exactly MAX_TITLE_LENGTH",
        longTitleNote.title(),
        equalTo(expectedLongTitle));
    assertThat(longTitleNote.title().length(), equalTo(Note.MAX_TITLE_LENGTH));
    assertThat(
        longTitleNote.folderId(), equalTo(ids.trashBucketFolderIds().get(LONG_TITLE_BUCKET)));

    NoteSnapshot alreadyInTrash = afterById.get(ids.specialNoteIds().get("already-in-trash"));
    assertThat(
        "already-in-trash note is not relocated",
        alreadyInTrash.folderId(),
        equalTo(ids.trashBucketFolderIds().get(ALREADY_IN_TRASH_BUCKET)));
    assertThat(
        "already-in-trash note keeps its title (no collision)",
        alreadyInTrash.title(),
        equalTo(bulkNoteTitle(ALREADY_IN_TRASH_BUCKET, ALREADY_IN_TRASH_INDEX)));
  }

  private void assertNoHistoryRowFor(Connection connection, String version) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT COUNT(*) AS c FROM flyway_schema_history WHERE version = ?")) {
      statement.setString(1, version);
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        assertThat("no Flyway history row yet for " + version, rs.getInt("c"), equalTo(0));
      }
    }
  }

  private void assertExactlyOneSuccessfulHistoryRowFor(Connection connection, String version)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT success FROM flyway_schema_history WHERE version = ? ORDER BY installed_rank"
                + " ASC")) {
      statement.setString(1, version);
      try (ResultSet rs = statement.executeQuery()) {
        assertThat("exactly one history row for " + version, rs.next(), is(true));
        assertThat(rs.getBoolean("success"), is(true));
        assertThat("no second history row for " + version, rs.next(), is(false));
      }
    }
  }

  private void assertBindingReplaced(Connection connection, int notebookId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT accepted_git_object_id FROM notebook_git_binding WHERE notebook_id = ?")) {
      statement.setInt(1, notebookId);
      try (ResultSet rs = statement.executeQuery()) {
        assertThat("a binding row must exist for notebook " + notebookId, rs.next(), is(true));
        String objectId = rs.getString("accepted_git_object_id");
        assertThat(
            "notebook " + notebookId + " binding was rebuilt with a new head",
            objectId,
            is(not(equalTo(LARGE_GIT_OBJECT_ID))));
        assertThat(
            "notebook " + notebookId + " binding was rebuilt with a new head",
            objectId,
            is(not(equalTo(SMALL_LIVE_GIT_OBJECT_ID))));
      }
    }
  }

  private void assertBindingUntouched(
      Connection connection, int notebookId, String expectedObjectId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT accepted_git_object_id FROM notebook_git_binding WHERE notebook_id = ?")) {
      statement.setInt(1, notebookId);
      try (ResultSet rs = statement.executeQuery()) {
        assertThat("a binding row must exist for notebook " + notebookId, rs.next(), is(true));
        assertThat(
            "a soft-deleted notebook's binding is left completely untouched by V300000327",
            rs.getString("accepted_git_object_id"),
            equalTo(expectedObjectId));
      }
    }
  }

  private void assertNotebookTreeMatchesDbGenerically(Connection connection, int notebookId)
      throws Exception {
    String acceptedGitObjectId;
    byte[] bundleBytes;
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT accepted_git_object_id, bundle_bytes FROM notebook_git_binding WHERE"
                + " notebook_id = ?")) {
      statement.setInt(1, notebookId);
      try (ResultSet rs = statement.executeQuery()) {
        assertThat(rs.next(), is(true));
        acceptedGitObjectId = rs.getString("accepted_git_object_id");
        bundleBytes = rs.getBytes("bundle_bytes");
      }
    }

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, bundleBytes);
      assertThat(headObjectId.getName(), equalTo(acceptedGitObjectId));
      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(
            "rebuilt baseline has exactly one parentless root commit",
            commit.getParentCount(),
            equalTo(0));
        List<PortableTreeEntry> actual = readTreeEntries(readBack, commit);

        SingleConnectionDataSource singleConnectionDataSource =
            new SingleConnectionDataSource(connection, true);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(singleConnectionDataSource);
        List<PortableTreeEntry> expected =
            currentPortableTreeFromDb(jdbcTemplate, notebookId).stream()
                .sorted((a, b) -> a.path().compareTo(b.path()))
                .toList();
        assertThat(
            "complete Portable tree for notebook " + notebookId + " matches current DB content",
            actual,
            contains(expected.toArray(new PortableTreeEntry[0])));
      }
    }
  }
}
