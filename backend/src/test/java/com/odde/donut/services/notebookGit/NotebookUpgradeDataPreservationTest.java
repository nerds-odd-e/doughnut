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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Rehearsal of the actual registered Flyway upgrade (V300000326-V300000328) against a populated,
 * pre-326 fixture seeded via raw JDBC into {@link PreUpgradeFixtureSchema}'s owned schema —
 * exercising the real migration chain and Flyway ordering instead of re-running any one migration's
 * operation directly against an already-fully-migrated live suite schema (the earlier shape of this
 * test, which only replayed V300000327's rebuild in isolation because the suite schema had already
 * dropped {@code note.deleted_at}).
 *
 * <p>Fixture coverage, across three notebooks owned by three different users:
 *
 * <ul>
 *   <li><b>Notebook A (bound, live):</b> active notes in nested folders carrying folder readme
 *       content, an existing {@code _trash} subtree (already containing a note at the exact
 *       destination path a to-be-converted note will land on, forcing collision-suffixing), a
 *       legacy soft-deleted note directly under the existing {@code _trash} root (no relocation
 *       expected, only {@code deleted_at} clearing), authored wiki-link references, learning/recall
 *       history, an independent tracking preference ({@code removed_from_tracking}) linked to a
 *       note, and a pre-existing {@code notebook_git_binding} that V300000327 must rebuild.
 *   <li><b>Notebook B (unbound, live):</b> an active note and a legacy soft-deleted note with no
 *       folder at all, and no {@code notebook_git_binding} row — proves V300000326's conversion is
 *       not narrowed to notebooks that happen to have a Git binding.
 *   <li><b>Notebook C (bound, soft-deleted notebook):</b> an active note and a legacy soft-deleted
 *       note, plus a pre-existing binding — proves V300000326 still converts a deleted notebook's
 *       notes (not narrowed to live notebooks), while V300000327 intentionally leaves a deleted
 *       notebook's binding completely untouched (by design; see its {@code WHERE n.deleted_at IS
 *       NULL} filter), so this binding must remain byte-for-byte unchanged.
 * </ul>
 *
 * <p>Allowlist of intended conversion changes (everything else must be byte-for-byte unchanged):
 *
 * <ul>
 *   <li>{@code note.folder_id} may change for legacy soft-deleted rows, to their computed trash
 *       destination (unchanged when the note already sits under an existing {@code _trash} root).
 *   <li>{@code note.title} may change for legacy soft-deleted rows, collision-suffixed with {@code
 *       " (N)"} only when the destination title is already occupied.
 *   <li>{@code note.deleted_at} is cleared to {@code NULL} for legacy soft-deleted rows, and the
 *       column itself is dropped for every row by V300000328.
 *   <li>{@code note.updated_at} is bumped for legacy soft-deleted rows only (V300000326's own
 *       {@code UPDATE}); every other note's {@code updated_at} stays exactly as seeded.
 *   <li>New {@code folder} rows may be inserted under a new or existing {@code _trash} root; every
 *       folder that existed before the upgrade keeps its id/parent/name/readme/created_at
 *       unchanged.
 *   <li>{@code notebook_git_binding.accepted_git_object_id}, {@code .bundle_bytes} and {@code
 *       .updated_at} are replaced for a live, already-bound notebook; {@code id}, {@code
 *       notebook_id} and {@code created_at} are preserved. A soft-deleted notebook's binding is
 *       left completely untouched.
 *   <li>{@code note.idx_note_structural_peer} is dropped and recreated without {@code deleted_at}
 *       by V300000328 (a schema-only change; row content proof is via slice 2's dedicated boundary
 *       test, not repeated here).
 * </ul>
 *
 * <p>Every {@code memory_tracker} and {@code authored_note_reference} row, and every note not a
 * legacy soft-deleted candidate, must be byte-for-byte identical before and after the whole chain.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotebookUpgradeDataPreservationTest {

  @Autowired DataSource dataSource;

  private static final String OLD_BOUND_GIT_OBJECT_ID = "a".repeat(40);
  private static final String DELETED_NOTEBOOK_GIT_OBJECT_ID = "c".repeat(40);

  @Test
  void populatedPreUpgradeNotebooksSurviveTheActualFlyway326To328Chain() throws Exception {
    String ownerSchemaName;
    try (Connection connection = dataSource.getConnection()) {
      ownerSchemaName = connection.getCatalog();
    }

    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      Connection connection = fixture.connection();
      FixtureIds ids = seedFixture(connection);

      List<NoteRow> notesBefore = readNotesWithDeletedAt(connection);
      List<FolderRow> foldersBefore = readFolders(connection);
      List<MemoryTrackerRow> trackersBefore = readMemoryTrackers(connection);
      List<AuthoredReferenceRow> referencesBefore = readAuthoredReferences(connection);
      List<BindingRow> bindingsBefore = readBindings(connection);

      // Run the actual registered 326 (legacy-trash conversion) and 327 (baseline rebuild) for
      // real, stopping short of 328 so note.deleted_at is still queryable for the conversion proof.
      fixture.flywayConfig().target(MigrationVersion.fromVersion("300000327")).load().migrate();

      List<NoteRow> notesAfterConversion = readNotesWithDeletedAt(connection);
      List<FolderRow> foldersAfterConversion = readFolders(connection);
      List<MemoryTrackerRow> trackersAfterConversion = readMemoryTrackers(connection);
      List<AuthoredReferenceRow> referencesAfterConversion = readAuthoredReferences(connection);
      List<BindingRow> bindingsAfterConversion = readBindings(connection);

      assertNotesFollowAllowlist(notesBefore, notesAfterConversion, ids);
      assertFoldersOnlyGainExpectedTrashRoots(foldersBefore, foldersAfterConversion, ids);
      assertThat(trackersAfterConversion, equalTo(trackersBefore));
      assertThat(referencesAfterConversion, equalTo(referencesBefore));
      assertBoundLiveNotebookBindingWasRebuilt(bindingsBefore, bindingsAfterConversion, ids);
      assertDeletedNotebookBindingUntouched(bindingsBefore, bindingsAfterConversion, ids);
      assertNoBindingForUnboundNotebook(bindingsAfterConversion, ids);
      assertNotebookATreeIndependentlyMatchesExpectedPaths(connection, ids);

      // Now run the actual registered V300000328 (schema-only column/index retirement) and confirm
      // it changes nothing but the schema: every retained row must equal its post-conversion state.
      fixture.flywayConfig().load().migrate();

      List<NoteRowFinal> notesFinal = readNotesFinal(connection);
      List<FolderRow> foldersFinal = readFolders(connection);
      List<MemoryTrackerRow> trackersFinal = readMemoryTrackers(connection);
      List<AuthoredReferenceRow> referencesFinal = readAuthoredReferences(connection);
      List<BindingRow> bindingsFinal = readBindings(connection);

      assertThat(notesFinal, equalTo(notesAfterConversion.stream().map(NoteRow::toFinal).toList()));
      assertThat(foldersFinal, equalTo(foldersAfterConversion));
      assertThat(trackersFinal, equalTo(trackersAfterConversion));
      assertThat(referencesFinal, equalTo(referencesAfterConversion));
      assertThat(bindingsFinal, equalTo(bindingsAfterConversion));
      assertNoteDeletedAtColumnIsGone(connection);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Fixture seeding
  // ---------------------------------------------------------------------------------------------

  private record FixtureIds(
      int notebookA,
      int notebookB,
      int notebookC,
      int trashItalianId,
      int existingTrashNoteId,
      int oldPastaLegacyId,
      int alreadyInTrashNoteId,
      int unboundDeletedNoteId,
      int deletedNotebookDeletedNoteId) {}

  private FixtureIds seedFixture(Connection connection) throws SQLException {
    int ownerA = insertUser(connection, "Owner A", "owner-a");
    int ownerB = insertUser(connection, "Owner B", "owner-b");
    int ownerC = insertUser(connection, "Owner C", "owner-c");

    int notebookA = insertNotebook(connection, ownerA, "Recipes Notebook", false);
    int notebookB = insertNotebook(connection, ownerB, "Unbound Notebook", false);
    int notebookC = insertNotebook(connection, ownerC, "Deleted Notebook", true);

    // Notebook A: nested live folders plus an existing _trash subtree. Recipes/Italian carry
    // readme content so the migration's preservation of folder readmes (not just notes) is
    // actually exercised, not merely trivially true because every folder's readme is NULL.
    int recipes = insertFolder(connection, notebookA, null, "Recipes", "# Recipes readme");
    int italian = insertFolder(connection, notebookA, recipes, "Italian", "# Italian readme");
    int trashRoot = insertFolder(connection, notebookA, null, "_trash", null);
    int trashRecipes = insertFolder(connection, notebookA, trashRoot, "Recipes", null);
    int trashItalian = insertFolder(connection, notebookA, trashRecipes, "Italian", null);

    int pasta = insertNote(connection, notebookA, italian, "Pasta", "Boil water and salt", null);
    int salad = insertNote(connection, notebookA, recipes, "Salad", "Toss leaves", null);
    int existingTrashNote =
        insertNote(
            connection, notebookA, trashItalian, "Old Pasta", "Existing trashed content", null);
    // Occupies the exact destination this legacy-deleted note will convert to, forcing suffixing.
    int oldPastaLegacy =
        insertNote(
            connection, notebookA, italian, "Old Pasta", "Legacy boiled pasta", legacyDeletedAt());
    // Already directly under the existing _trash root: only deleted_at should clear, no move.
    int alreadyInTrashNote =
        insertNote(
            connection,
            notebookA,
            trashRoot,
            "Already In Trash Note",
            "Already trashed, no relocation",
            legacyDeletedAt());

    insertMemoryTracker(
        connection, ownerA, pasta, "UNDERSTANDING", "", 2.5f, 1.2f, false); // recall history
    insertMemoryTracker(
        connection,
        ownerA,
        salad,
        "SPELLING",
        "salad",
        0f,
        null,
        true); // independent tracking preference
    insertMemoryTracker(connection, ownerA, oldPastaLegacy, "UNDERSTANDING", "", 1.0f, 0.5f, false);

    insertAuthoredReference(connection, salad, "Recipes/Italian/Pasta", "Pasta");

    insertBinding(connection, notebookA, OLD_BOUND_GIT_OBJECT_ID);

    // Notebook B: unbound, no folders at all.
    insertNote(connection, notebookB, null, "Standalone", "Nothing to trash", null);
    int unboundDeletedNote =
        insertNote(connection, notebookB, null, "Deleted In Unbound", "Gone", legacyDeletedAt());

    // Notebook C: soft-deleted notebook, still bound.
    insertNote(connection, notebookC, null, "Kept Active In Deleted Notebook", "Still here", null);
    int deletedNotebookDeletedNote =
        insertNote(
            connection,
            notebookC,
            null,
            "Deleted In Deleted Notebook",
            "Also gone",
            legacyDeletedAt());
    insertBinding(connection, notebookC, DELETED_NOTEBOOK_GIT_OBJECT_ID);

    return new FixtureIds(
        notebookA,
        notebookB,
        notebookC,
        trashItalian,
        existingTrashNote,
        oldPastaLegacy,
        alreadyInTrashNote,
        unboundDeletedNote,
        deletedNotebookDeletedNote);
  }

  private void insertMemoryTracker(
      Connection connection,
      int userId,
      int noteId,
      String type,
      String propertyKey,
      Float stability,
      Float difficulty,
      boolean removedFromTracking)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "INSERT INTO memory_tracker (user_id, note_id, type, property_key, stability,"
              + " difficulty, next_recall_at, last_recalled_at, assimilated_at,"
              + " removed_from_tracking) VALUES ("
              + userId
              + ", "
              + noteId
              + ", '"
              + type
              + "', '"
              + propertyKey
              + "', "
              + stability
              + ", "
              + (difficulty == null ? "NULL" : difficulty)
              + ", '2026-01-01 00:00:00', '2025-12-01 00:00:00', '2025-11-01 00:00:00', "
              + (removedFromTracking ? 1 : 0)
              + ")");
    }
  }

  private void insertAuthoredReference(
      Connection connection, int sourceNoteId, String wikiNotePortion, String displayText)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "INSERT INTO authored_note_reference (source_note_id, kind, authored_link,"
              + " display_text, document_order, wiki_note_portion) VALUES ("
              + sourceNoteId
              + ", 'WIKI_PORTABLE_PATH', '"
              + wikiNotePortion
              + "', '"
              + displayText
              + "', 0, '"
              + wikiNotePortion
              + "')");
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Row capture
  // ---------------------------------------------------------------------------------------------

  private record NoteRow(
      int id,
      int notebookId,
      Integer folderId,
      String title,
      String content,
      Timestamp deletedAt,
      Timestamp createdAt,
      Timestamp updatedAt) {
    NoteRowFinal toFinal() {
      return new NoteRowFinal(id, notebookId, folderId, title, content, createdAt, updatedAt);
    }
  }

  private record NoteRowFinal(
      int id,
      int notebookId,
      Integer folderId,
      String title,
      String content,
      Timestamp createdAt,
      Timestamp updatedAt) {}

  private record FolderRow(
      int id, int notebookId, Integer parentFolderId, String name, String readmeContent) {}

  private record MemoryTrackerRow(
      int id,
      int userId,
      int noteId,
      String type,
      String propertyKey,
      float stability,
      Float difficulty,
      Timestamp nextRecallAt,
      Timestamp lastRecalledAt,
      Timestamp assimilatedAt,
      boolean removedFromTracking) {}

  private record AuthoredReferenceRow(
      int id,
      int sourceNoteId,
      String kind,
      String authoredLink,
      String displayText,
      int documentOrder,
      String wikiNotePortion) {}

  private record BindingRow(
      int id,
      int notebookId,
      String acceptedGitObjectId,
      List<Byte> bundleBytes,
      Timestamp createdAt,
      Timestamp updatedAt) {}

  private List<NoteRow> readNotesWithDeletedAt(Connection connection) throws SQLException {
    List<NoteRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, notebook_id, folder_id, title, content, deleted_at, created_at,"
                    + " updated_at FROM note ORDER BY id ASC")) {
      while (rs.next()) {
        rows.add(
            new NoteRow(
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

  private List<NoteRowFinal> readNotesFinal(Connection connection) throws SQLException {
    List<NoteRowFinal> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, notebook_id, folder_id, title, content, created_at, updated_at"
                    + " FROM note ORDER BY id ASC")) {
      while (rs.next()) {
        rows.add(
            new NoteRowFinal(
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

  private List<FolderRow> readFolders(Connection connection) throws SQLException {
    List<FolderRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, notebook_id, parent_folder_id, name, readme_content FROM folder"
                    + " ORDER BY id ASC")) {
      while (rs.next()) {
        rows.add(
            new FolderRow(
                rs.getInt("id"),
                rs.getInt("notebook_id"),
                nullableInt(rs, "parent_folder_id"),
                rs.getString("name"),
                rs.getString("readme_content")));
      }
    }
    return rows;
  }

  private List<MemoryTrackerRow> readMemoryTrackers(Connection connection) throws SQLException {
    List<MemoryTrackerRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, user_id, note_id, type, property_key, stability, difficulty,"
                    + " next_recall_at, last_recalled_at, assimilated_at, removed_from_tracking"
                    + " FROM memory_tracker ORDER BY id ASC")) {
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
                rs.getTimestamp("next_recall_at"),
                rs.getTimestamp("last_recalled_at"),
                rs.getTimestamp("assimilated_at"),
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
                "SELECT id, source_note_id, kind, authored_link, display_text, document_order,"
                    + " wiki_note_portion FROM authored_note_reference ORDER BY id ASC")) {
      while (rs.next()) {
        rows.add(
            new AuthoredReferenceRow(
                rs.getInt("id"),
                rs.getInt("source_note_id"),
                rs.getString("kind"),
                rs.getString("authored_link"),
                rs.getString("display_text"),
                rs.getInt("document_order"),
                rs.getString("wiki_note_portion")));
      }
    }
    return rows;
  }

  private List<BindingRow> readBindings(Connection connection) throws SQLException {
    List<BindingRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT id, notebook_id, accepted_git_object_id, bundle_bytes, created_at,"
                    + " updated_at FROM notebook_git_binding ORDER BY id ASC")) {
      while (rs.next()) {
        byte[] bytes = rs.getBytes("bundle_bytes");
        List<Byte> boxed = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
          boxed.add(b);
        }
        rows.add(
            new BindingRow(
                rs.getInt("id"),
                rs.getInt("notebook_id"),
                rs.getString("accepted_git_object_id"),
                boxed,
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")));
      }
    }
    return rows;
  }

  // ---------------------------------------------------------------------------------------------
  // Assertions
  // ---------------------------------------------------------------------------------------------

  private void assertNotesFollowAllowlist(
      List<NoteRow> before, List<NoteRow> afterConversion, FixtureIds ids) {
    Map<Integer, NoteRow> beforeById =
        before.stream().collect(Collectors.toMap(NoteRow::id, r -> r));
    Map<Integer, NoteRow> afterById =
        afterConversion.stream().collect(Collectors.toMap(NoteRow::id, r -> r));

    java.util.Set<Integer> touchedIds =
        java.util.Set.of(
            ids.oldPastaLegacyId(),
            ids.alreadyInTrashNoteId(),
            ids.unboundDeletedNoteId(),
            ids.deletedNotebookDeletedNoteId());

    for (Integer id : beforeById.keySet()) {
      NoteRow b = beforeById.get(id);
      NoteRow a = afterById.get(id);
      assertThat("note " + id + " must still exist", a, is(not(nullValue())));
      assertThat(
          "note " + id + " id/notebook/content unchanged", a.notebookId(), equalTo(b.notebookId()));
      assertThat("note " + id + " content unchanged", a.content(), equalTo(b.content()));
      assertThat("note " + id + " created_at unchanged", a.createdAt(), equalTo(b.createdAt()));

      if (touchedIds.contains(id)) {
        assertThat(
            "legacy note " + id + " was soft-deleted before", b.deletedAt(), is(not(nullValue())));
        assertThat("legacy note " + id + " deleted_at cleared", a.deletedAt(), is(nullValue()));
        assertThat(
            "legacy note " + id + " updated_at bumped by the conversion",
            a.updatedAt(),
            is(not(equalTo(b.updatedAt()))));
      } else {
        assertThat(
            "untouched note " + id + " deleted_at unchanged",
            a.deletedAt(),
            equalTo(b.deletedAt()));
        assertThat("untouched note " + id + " title unchanged", a.title(), equalTo(b.title()));
        assertThat(
            "untouched note " + id + " folder unchanged", a.folderId(), equalTo(b.folderId()));
        assertThat(
            "untouched note " + id + " updated_at unchanged",
            a.updatedAt(),
            equalTo(b.updatedAt()));
      }
    }

    // Existing folder/binding identity reused: the collision case relocates into the pre-existing
    // trash folder (not a newly created one) and gets a collision-suffixed title.
    NoteRow relocated = afterById.get(ids.oldPastaLegacyId());
    assertThat(relocated.folderId(), equalTo(ids.trashItalianId()));
    assertThat(relocated.title(), equalTo("Old Pasta (2)"));

    // Already under the existing _trash root: no relocation, no title change.
    NoteRow alreadyTrashed = afterById.get(ids.alreadyInTrashNoteId());
    assertThat(
        alreadyTrashed.folderId(), equalTo(beforeById.get(ids.alreadyInTrashNoteId()).folderId()));
    assertThat(alreadyTrashed.title(), equalTo("Already In Trash Note"));

    // Pre-existing trash note that occupied the collision destination stays completely untouched.
    NoteRow existingTrash = afterById.get(ids.existingTrashNoteId());
    assertThat(existingTrash.title(), equalTo("Old Pasta"));

    // Unbound and deleted-notebook legacy notes were still converted: relocated into a NEW trash
    // root created for their own notebook (they had no folder at all before).
    NoteRow unboundConverted = afterById.get(ids.unboundDeletedNoteId());
    assertThat(unboundConverted.folderId(), is(not(nullValue())));
    NoteRow deletedNotebookConverted = afterById.get(ids.deletedNotebookDeletedNoteId());
    assertThat(deletedNotebookConverted.folderId(), is(not(nullValue())));
  }

  private void assertFoldersOnlyGainExpectedTrashRoots(
      List<FolderRow> before, List<FolderRow> afterConversion, FixtureIds ids) {
    Map<Integer, FolderRow> beforeById =
        before.stream().collect(Collectors.toMap(FolderRow::id, r -> r));
    Map<Integer, FolderRow> afterById =
        afterConversion.stream().collect(Collectors.toMap(FolderRow::id, r -> r));

    for (Integer id : beforeById.keySet()) {
      assertThat(
          "pre-existing folder " + id + " preserved unchanged",
          afterById.get(id),
          equalTo(beforeById.get(id)));
    }

    List<FolderRow> newFolders =
        afterConversion.stream().filter(f -> !beforeById.containsKey(f.id())).toList();
    assertThat(
        "exactly one new trash root per notebook needing one", newFolders.size(), equalTo(2));
    for (FolderRow created : newFolders) {
      assertThat(created.name(), equalTo("_trash"));
      assertThat(created.parentFolderId(), is(nullValue()));
      assertThat(
          "new trash root belongs to notebook B or C",
          created.notebookId() == ids.notebookB() || created.notebookId() == ids.notebookC(),
          is(true));
    }
  }

  private void assertBoundLiveNotebookBindingWasRebuilt(
      List<BindingRow> before, List<BindingRow> after, FixtureIds ids) {
    BindingRow beforeRow =
        before.stream().filter(b -> b.notebookId() == ids.notebookA()).findFirst().orElseThrow();
    BindingRow afterRow =
        after.stream().filter(b -> b.notebookId() == ids.notebookA()).findFirst().orElseThrow();
    assertThat("binding id preserved (not recreated)", afterRow.id(), equalTo(beforeRow.id()));
    assertThat(afterRow.notebookId(), equalTo(beforeRow.notebookId()));
    assertThat(
        "created_at preserved across rebuild",
        afterRow.createdAt(),
        equalTo(beforeRow.createdAt()));
    assertThat(
        "accepted_git_object_id replaced by rebuild",
        afterRow.acceptedGitObjectId(),
        is(not(equalTo(beforeRow.acceptedGitObjectId()))));
    assertThat(
        "bundle_bytes replaced by rebuild",
        afterRow.bundleBytes(),
        is(not(equalTo(beforeRow.bundleBytes()))));
  }

  private void assertDeletedNotebookBindingUntouched(
      List<BindingRow> before, List<BindingRow> after, FixtureIds ids) {
    BindingRow beforeRow =
        before.stream().filter(b -> b.notebookId() == ids.notebookC()).findFirst().orElseThrow();
    BindingRow afterRow =
        after.stream().filter(b -> b.notebookId() == ids.notebookC()).findFirst().orElseThrow();
    assertThat(
        "a soft-deleted notebook's binding is left completely untouched by V300000327",
        afterRow,
        equalTo(beforeRow));
    assertThat(afterRow.acceptedGitObjectId(), equalTo(DELETED_NOTEBOOK_GIT_OBJECT_ID));
  }

  private void assertNoBindingForUnboundNotebook(List<BindingRow> after, FixtureIds ids) {
    assertThat(after.stream().noneMatch(b -> b.notebookId() == ids.notebookB()), is(true));
  }

  private void assertNotebookATreeIndependentlyMatchesExpectedPaths(
      Connection connection, FixtureIds ids) throws Exception {
    String acceptedGitObjectId;
    byte[] bundleBytes;
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT accepted_git_object_id, bundle_bytes FROM notebook_git_binding"
                    + " WHERE notebook_id = "
                    + ids.notebookA())) {
      assertThat(rs.next(), is(true));
      acceptedGitObjectId = rs.getString("accepted_git_object_id");
      bundleBytes = rs.getBytes("bundle_bytes");
    }

    List<PortableTreeEntry> expected =
        List.of(
                new PortableTreeEntry("Recipes/Italian/Pasta.md", "Boil water and salt"),
                new PortableTreeEntry("Recipes/Salad.md", "Toss leaves"),
                new PortableTreeEntry(
                    "Recipes/README.md", ExportReadmeMarkdown.assemble("# Recipes readme")),
                new PortableTreeEntry(
                    "Recipes/Italian/README.md", ExportReadmeMarkdown.assemble("# Italian readme")),
                new PortableTreeEntry(
                    "_trash/Already In Trash Note.md", "Already trashed, no relocation"),
                new PortableTreeEntry(
                    "_trash/Recipes/Italian/Old Pasta (2).md", "Legacy boiled pasta"),
                new PortableTreeEntry(
                    "_trash/Recipes/Italian/Old Pasta.md", "Existing trashed content"))
            .stream()
            .sorted((a, b) -> a.path().compareTo(b.path()))
            .toList();

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
        assertThat(actual, contains(expected.toArray(new PortableTreeEntry[0])));

        // Cross-check with the shared DB-derived snapshot helper (reused, not reimplemented).
        // suppressClose=true so the fixture's own connection lifecycle is unaffected; this
        // adapter is never closed itself, only used to hand the raw connection to JdbcTemplate.
        SingleConnectionDataSource singleConnectionDataSource =
            new SingleConnectionDataSource(connection, true);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(singleConnectionDataSource);
        List<PortableTreeEntry> fromDb =
            currentPortableTreeFromDb(jdbcTemplate, ids.notebookA()).stream()
                .sorted((a, b) -> a.path().compareTo(b.path()))
                .toList();
        assertThat(actual, contains(fromDb.toArray(new PortableTreeEntry[0])));
      }
    }
  }

  private void assertNoteDeletedAtColumnIsGone(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SHOW COLUMNS FROM note LIKE 'deleted_at'")) {
      assertThat("note.deleted_at must be gone after V300000328", rs.next(), is(false));
    }
  }
}
