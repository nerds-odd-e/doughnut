package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataMigrationBatchWorker {

  private final NoteRepository noteRepository;
  private final WikiSlugPathService wikiSlugPathService;
  private final EntityPersister entityPersister;
  private final WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final JdbcTemplate jdbcTemplate;
  private final AdminDataMigrationProgressPopulator progressPopulator;

  public AdminDataMigrationBatchWorker(
      NoteRepository noteRepository,
      WikiSlugPathService wikiSlugPathService,
      EntityPersister entityPersister,
      WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService,
      WikiTitleCacheService wikiTitleCacheService,
      JdbcTemplate jdbcTemplate,
      @Lazy AdminDataMigrationProgressPopulator progressPopulator) {
    this.noteRepository = noteRepository;
    this.wikiSlugPathService = wikiSlugPathService;
    this.entityPersister = entityPersister;
    this.wikiReferenceMigrationProgressService = wikiReferenceMigrationProgressService;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.jdbcTemplate = jdbcTemplate;
    this.progressPopulator = progressPopulator;
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminDataMigrationStatusDTO executeBatch(User adminUser) {
    if (!wikiStepCompleted(AdminDataMigrationService.STEP_RELATIONSHIP_WIKI_BACKFILL)) {
      return runRelationshipWikiBackfillBatch(adminUser);
    }
    if (!wikiStepCompleted(AdminDataMigrationService.STEP_LEGACY_PARENT_FRONTMATTER)) {
      return runLegacyParentFrontmatterBatch(adminUser);
    }
    return runSlugRegenerationBatch();
  }

  private boolean wikiStepCompleted(String stepName) {
    return wikiReferenceMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }

  private AdminDataMigrationStatusDTO runRelationshipWikiBackfillBatch(User adminUser) {
    String step = AdminDataMigrationService.STEP_RELATIONSHIP_WIKI_BACKFILL;
    long totalEligible = noteRepository.countRelationshipNotesEligibleForWikiReferenceMigration();
    wikiReferenceMigrationProgressService.startOrResume(step, totalCountForProgress(totalEligible));
    Integer exclusiveAfter = exclusiveLastProcessedNoteId(step);
    List<Integer> batchIds =
        noteRepository.findRelationshipWikiMigrationCandidateIdsExclusiveAfterAsc(
            exclusiveAfter,
            PageRequest.of(0, AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE));
    if (batchIds.isEmpty()) {
      wikiReferenceMigrationProgressService.markCompleted(step);
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage("Relationship wiki backfill: nothing pending.");
      progressPopulator.populateMigrationProgress(dto);
      return dto;
    }
    List<Note> loaded = noteRepository.hydrateRelationshipWikiMigrationNotesByIds(batchIds);
    Map<Integer, Note> byId =
        loaded.stream().collect(Collectors.toMap(Note::getId, Function.identity()));
    for (Note relation : notesForBatchIdsInOrder(batchIds, byId)) {
      if (relation.getTitle() == null || relation.getTitle().trim().isEmpty()) {
        relation.setTitle(
            RelationshipNoteTitleFormatter.format(
                relation.getParent().getTitle(),
                relation.getRelationType().label,
                relation.getTargetNote().getTitle()));
      }
      if (needsRelationshipDetailsMigration(relation)) {
        relation.setDetails(
            RelationshipNoteMarkdownFormatter.format(
                relation.getRelationType(),
                relation.getParent().getTitle(),
                relation.getTargetNote().getTitle(),
                relation.getDetails()));
      }
      wikiSlugPathService.assignSlugForNewNote(relation);
      User viewer = viewerForWikiTitleCacheRefresh(relation, adminUser);
      refreshWikiTitleCacheForRelationshipMigration(relation, viewer);
      entityPersister.merge(relation);
      entityPersister.flush();
    }
    entityPersister.flush();
    int lastId = batchIds.get(batchIds.size() - 1);
    wikiReferenceMigrationProgressService.recordBatchSuccess(step, lastId, batchIds.size());
    if (noteRepository
        .findRelationshipWikiMigrationCandidateIdsExclusiveAfterAsc(lastId, PageRequest.of(0, 1))
        .isEmpty()) {
      wikiReferenceMigrationProgressService.markCompleted(step);
    }
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(
        "Relationship wiki backfill: processed %d note(s) in this batch."
            .formatted(batchIds.size()));
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private static boolean needsRelationshipDetailsMigration(Note relation) {
    String d = relation.getDetails();
    return d == null || d.trim().isEmpty() || !d.contains("type: relationship");
  }

  /**
   * Wiki link resolution uses notebook read access; use notebook owner or a circle member when
   * present, otherwise the admin running the migration.
   */
  private AdminDataMigrationStatusDTO runLegacyParentFrontmatterBatch(User adminUser) {
    String step = AdminDataMigrationService.STEP_LEGACY_PARENT_FRONTMATTER;
    entityPersister.flush();
    long total = countLegacyChildNotesEligibleForWikiMigration();
    wikiReferenceMigrationProgressService.startOrResume(step, totalCountForProgress(total));
    Integer exclusiveAfter = exclusiveLastProcessedNoteId(step);
    List<Integer> batchIds =
        nextLegacyChildNoteIdsPage(
            exclusiveAfter, AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE);
    if (batchIds.isEmpty()) {
      wikiReferenceMigrationProgressService.markCompleted(step);
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage("Legacy parent frontmatter: nothing pending.");
      progressPopulator.populateMigrationProgress(dto);
      return dto;
    }
    for (Integer id : batchIds) {
      Note note = noteRepository.findById(id).orElseThrow();
      Note parent = note.getParent();
      String parentTitle = parent != null ? parent.getTitle() : null;
      String merged =
          LegacyParentWikiFrontmatterMerge.mergeParentWikiLink(note.getDetails(), parentTitle);
      if (!Objects.equals(merged, note.getDetails())) {
        note.setDetails(merged);
      }
      entityPersister.merge(note);
    }
    entityPersister.flush();
    for (Integer id : batchIds) {
      refreshWikiTitleCacheForLegacyMigration(id, adminUser);
    }
    int lastId = batchIds.get(batchIds.size() - 1);
    wikiReferenceMigrationProgressService.recordBatchSuccess(step, lastId, batchIds.size());
    if (nextLegacyChildNoteIdsPage(lastId, 1).isEmpty()) {
      wikiReferenceMigrationProgressService.markCompleted(step);
    }
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(
        "Legacy parent frontmatter: processed %d note(s) in this batch."
            .formatted(batchIds.size()));
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private static User viewerForWikiTitleCacheRefresh(Note relationshipNote, User adminUser) {
    Ownership ownership = relationshipNote.getNotebook().getOwnership();
    if (ownership.getUser() != null) {
      return ownership.getUser();
    }
    if (ownership.getCircle() != null && !ownership.getCircle().getMembers().isEmpty()) {
      return ownership.getCircle().getMembers().getFirst();
    }
    return adminUser;
  }

  /**
   * Clears stuck rows then retries once; JDBC avoids enqueueing {@link
   * com.odde.doughnut.entities.NoteWikiTitleCache} on the migration session.
   */
  private void refreshWikiTitleCacheForRelationshipMigration(Note note, User viewer) {
    try {
      wikiTitleCacheService.replaceWikiTitleCacheRowsJdbc(note, viewer);
    } catch (RuntimeException e) {
      jdbcTemplate.update("DELETE FROM note_wiki_title_cache WHERE note_id = ?", note.getId());
      wikiTitleCacheService.replaceWikiTitleCacheRowsJdbc(note, viewer);
    }
  }

  /**
   * Best-effort refresh; cursor still advances so migration can finish even if cache stays empty.
   */
  private void refreshWikiTitleCacheForLegacyMigration(int noteId, User adminUser) {
    Note note = noteRepository.findById(noteId).orElseThrow();
    User viewer = viewerForWikiTitleCacheRefresh(note, adminUser);
    try {
      wikiTitleCacheService.replaceWikiTitleCacheRowsJdbc(note, viewer);
    } catch (RuntimeException e) {
      jdbcTemplate.update("DELETE FROM note_wiki_title_cache WHERE note_id = ?", noteId);
      try {
        wikiTitleCacheService.replaceWikiTitleCacheRowsJdbc(note, viewer);
      } catch (RuntimeException ignored) {
        // Cache can be rebuilt when the note is loaded in the app.
      }
    }
  }

  private AdminDataMigrationStatusDTO runSlugRegenerationBatch() {
    String step = AdminDataMigrationService.STEP_NOTE_SLUG_PATH_REGENERATION;
    long total = noteRepository.countNonDeletedNotes();
    wikiReferenceMigrationProgressService.startOrResume(step, totalCountForProgress(total));
    Integer exclusiveAfter = exclusiveLastProcessedNoteId(step);
    List<Integer> batchIds =
        noteRepository.findNonDeletedNoteIdsExclusiveAfterAsc(
            exclusiveAfter,
            PageRequest.of(0, AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE));
    if (batchIds.isEmpty()) {
      wikiReferenceMigrationProgressService.markCompleted(step);
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage("Slug regeneration: nothing pending.");
      progressPopulator.populateMigrationProgress(dto);
      return dto;
    }
    List<Note> loaded = noteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(batchIds);
    Map<Integer, Note> byId =
        loaded.stream().collect(Collectors.toMap(Note::getId, Function.identity()));
    for (Note note : notesForBatchIdsInOrder(batchIds, byId)) {
      regenerateSlugForNoteInBatch(note, batchIds);
    }
    int lastId = batchIds.get(batchIds.size() - 1);
    wikiReferenceMigrationProgressService.recordBatchSuccess(step, lastId, batchIds.size());
    if (noteRepository
        .findNonDeletedNoteIdsExclusiveAfterAsc(lastId, PageRequest.of(0, 1))
        .isEmpty()) {
      wikiReferenceMigrationProgressService.markCompleted(step);
    }
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(
        "Slug regeneration: processed %d note(s) in this batch.".formatted(batchIds.size()));
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private void regenerateSlugForNoteInBatch(Note note, List<Integer> batchIds) {
    String oldSlug = note.getSlug();
    try {
      wikiSlugPathService.assignSlugForNewNote(note);
      entityPersister.merge(note);
      entityPersister.flush();
    } catch (RuntimeException e) {
      throw new IllegalStateException(slugRegenerationFailureDetails(note, oldSlug, batchIds), e);
    }
  }

  private static String slugRegenerationFailureDetails(
      Note note, String oldSlug, List<Integer> batchIds) {
    String assignedSlug = note.getSlug();
    String folderSlug = note.getFolder() == null ? null : note.getFolder().getSlug();
    Integer folderId = note.getFolder() == null ? null : note.getFolder().getId();
    return "slug-regeneration-note-failed marker=%s noteId=%s notebookId=%s folderId=%s"
        + " title=%s oldSlugLen=%d assignedSlugLen=%d folderSlugLen=%d oldSlug=%s assignedSlug=%s"
        + " folderSlug=%s batchIds=%s"
            .formatted(
                AdminDataMigrationService.DIAGNOSTIC_MARKER,
                note.getId(),
                note.getNotebook().getId(),
                folderId,
                preview(note.getTitle()),
                lengthOf(oldSlug),
                lengthOf(assignedSlug),
                lengthOf(folderSlug),
                preview(oldSlug),
                preview(assignedSlug),
                preview(folderSlug),
                batchIds);
  }

  private static int lengthOf(String value) {
    return value == null ? 0 : value.length();
  }

  private static String preview(String value) {
    if (value == null) {
      return "<null>";
    }
    if (value.length() <= 120) {
      return value;
    }
    return value.substring(0, 80) + "..." + value.substring(value.length() - 20);
  }

  private Integer exclusiveLastProcessedNoteId(String stepName) {
    return wikiReferenceMigrationProgressService
        .find(stepName)
        .map(WikiReferenceMigrationProgress::getLastProcessedNote)
        .filter(Objects::nonNull)
        .map(Note::getId)
        .orElse(null);
  }

  private static int totalCountForProgress(long total) {
    if (total > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) total;
  }

  private static List<Note> notesForBatchIdsInOrder(
      List<Integer> batchIds, Map<Integer, Note> byId) {
    return batchIds.stream().map(byId::get).filter(Objects::nonNull).toList();
  }

  private long countLegacyChildNotesEligibleForWikiMigration() {
    Long c =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note WHERE deleted_at IS NULL AND parent_id IS NOT NULL "
                + "AND target_note_id IS NULL",
            Long.class);
    return c == null ? 0 : c;
  }

  private List<Integer> nextLegacyChildNoteIdsPage(Integer exclusiveAfter, int limit) {
    if (exclusiveAfter == null) {
      return jdbcTemplate.query(
          "SELECT id FROM note WHERE deleted_at IS NULL AND parent_id IS NOT NULL "
              + "AND target_note_id IS NULL ORDER BY id ASC LIMIT ?",
          (rs, rowNum) -> rs.getInt(1),
          limit);
    }
    return jdbcTemplate.query(
        "SELECT id FROM note WHERE deleted_at IS NULL AND parent_id IS NOT NULL "
            + "AND target_note_id IS NULL AND id > ? ORDER BY id ASC LIMIT ?",
        (rs, rowNum) -> rs.getInt(1),
        exclusiveAfter,
        limit);
  }
}
