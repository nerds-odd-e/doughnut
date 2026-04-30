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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataMigrationService {

  public static final String READY_MESSAGE =
      "Wiki data migration: relationship wiki backfill (title, details, cache), then batched note"
          + " slug regeneration.";

  public static final String STEP_RELATIONSHIP_WIKI_BACKFILL = "relationship_wiki_backfill";
  public static final String STEP_NOTE_SLUG_PATH_REGENERATION = "note_slug_path_regeneration";

  /** Max notes processed per HTTP request for relationship wiki backfill or slug regeneration. */
  public static final int WIKI_REFERENCE_MIGRATION_BATCH_SIZE = 50;

  private final NoteRepository noteRepository;
  private final WikiSlugPathService wikiSlugPathService;
  private final EntityPersister entityPersister;
  private final WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService;
  private final WikiTitleCacheService wikiTitleCacheService;

  public AdminDataMigrationService(
      NoteRepository noteRepository,
      WikiSlugPathService wikiSlugPathService,
      EntityPersister entityPersister,
      WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService,
      WikiTitleCacheService wikiTitleCacheService) {
    this.noteRepository = noteRepository;
    this.wikiSlugPathService = wikiSlugPathService;
    this.entityPersister = entityPersister;
    this.wikiReferenceMigrationProgressService = wikiReferenceMigrationProgressService;
    this.wikiTitleCacheService = wikiTitleCacheService;
  }

  public AdminDataMigrationStatusDTO getStatus() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(READY_MESSAGE);
    populateMigrationProgress(dto);
    return dto;
  }

  @Transactional
  public AdminDataMigrationStatusDTO runBatch(User adminUser) {
    Optional<String> failedStep = findFailedStepName();
    if (failedStep.isPresent()) {
      return dtoAfterBlockedRun(
          "Migration is stopped after a failure; fix data and clear progress if needed.");
    }
    if (migrationFullyComplete()) {
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage("Wiki reference migration is already complete.");
      populateMigrationProgress(dto);
      return dto;
    }
    if (!stepCompleted(STEP_RELATIONSHIP_WIKI_BACKFILL)) {
      return runRelationshipWikiBackfillBatch(adminUser);
    }
    return runSlugRegenerationBatch();
  }

  private AdminDataMigrationStatusDTO runRelationshipWikiBackfillBatch(User adminUser) {
    String step = STEP_RELATIONSHIP_WIKI_BACKFILL;
    try {
      List<Note> candidates =
          noteRepository.findRelationshipNotesForWikiReferenceMigrationOrderByIdAsc();
      List<Integer> orderedIds = candidates.stream().map(Note::getId).toList();
      wikiReferenceMigrationProgressService.startOrResume(step, orderedIds.size());
      List<Integer> pending =
          wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
      if (pending.isEmpty()) {
        wikiReferenceMigrationProgressService.markCompleted(step);
        AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
        dto.setMessage("Relationship wiki backfill: nothing pending.");
        populateMigrationProgress(dto);
        return dto;
      }
      int take = Math.min(WIKI_REFERENCE_MIGRATION_BATCH_SIZE, pending.size());
      List<Integer> batch = pending.subList(0, take);
      Map<Integer, Note> byId =
          candidates.stream().collect(Collectors.toMap(Note::getId, Function.identity()));
      for (Integer id : batch) {
        Note relation = byId.get(id);
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
        wikiTitleCacheService.refreshForNote(
            relation, viewerForWikiTitleCacheRefresh(relation, adminUser));
        entityPersister.merge(relation);
      }
      entityPersister.flush();
      int lastId = batch.get(batch.size() - 1);
      wikiReferenceMigrationProgressService.recordBatchSuccess(step, lastId, batch.size());
      List<Integer> stillPending =
          wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
      if (stillPending.isEmpty()) {
        wikiReferenceMigrationProgressService.markCompleted(step);
      }
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage(
          "Relationship wiki backfill: processed %d note(s) in this batch."
              .formatted(batch.size()));
      populateMigrationProgress(dto);
      return dto;
    } catch (RuntimeException e) {
      wikiReferenceMigrationProgressService.markFailed(step, e.getMessage());
      return dtoAfterFailure(step, e.getMessage());
    }
  }

  private static boolean needsRelationshipDetailsMigration(Note relation) {
    String d = relation.getDetails();
    return d == null || d.trim().isEmpty() || !d.contains("type: relationship");
  }

  /**
   * Wiki link resolution uses notebook read access; use notebook owner or a circle member when
   * present, otherwise the admin running the migration.
   */
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

  private AdminDataMigrationStatusDTO runSlugRegenerationBatch() {
    String step = STEP_NOTE_SLUG_PATH_REGENERATION;
    try {
      List<Note> allNotes = noteRepository.findAllNonDeletedNotesOrderByIdAsc();
      List<Integer> orderedIds = allNotes.stream().map(Note::getId).toList();
      wikiReferenceMigrationProgressService.startOrResume(step, orderedIds.size());
      List<Integer> pending =
          wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
      if (pending.isEmpty()) {
        wikiReferenceMigrationProgressService.markCompleted(step);
        AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
        dto.setMessage("Slug regeneration: nothing pending.");
        populateMigrationProgress(dto);
        return dto;
      }
      int take = Math.min(WIKI_REFERENCE_MIGRATION_BATCH_SIZE, pending.size());
      List<Integer> batch = pending.subList(0, take);
      Map<Integer, Note> byId =
          allNotes.stream().collect(Collectors.toMap(Note::getId, Function.identity()));
      for (Integer id : batch) {
        Note note = byId.get(id);
        wikiSlugPathService.assignSlugForNewNote(note);
        entityPersister.merge(note);
      }
      entityPersister.flush();
      int lastId = batch.get(batch.size() - 1);
      wikiReferenceMigrationProgressService.recordBatchSuccess(step, lastId, batch.size());
      List<Integer> stillPending =
          wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
      if (stillPending.isEmpty()) {
        wikiReferenceMigrationProgressService.markCompleted(step);
      }
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage(
          "Slug regeneration: processed %d note(s) in this batch.".formatted(batch.size()));
      populateMigrationProgress(dto);
      return dto;
    } catch (RuntimeException e) {
      wikiReferenceMigrationProgressService.markFailed(step, e.getMessage());
      return dtoAfterFailure(step, e.getMessage());
    }
  }

  private AdminDataMigrationStatusDTO dtoAfterBlockedRun(String message) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(message);
    populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO dtoAfterFailure(String step, String message) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Migration batch failed: " + message);
    populateMigrationProgress(dto);
    return dto;
  }

  private void populateMigrationProgress(AdminDataMigrationStatusDTO dto) {
    Optional<WikiReferenceMigrationProgress> failed =
        List.of(STEP_RELATIONSHIP_WIKI_BACKFILL, STEP_NOTE_SLUG_PATH_REGENERATION).stream()
            .flatMap(s -> wikiReferenceMigrationProgressService.find(s).stream())
            .filter(p -> p.getStatus() == WikiReferenceMigrationStepStatus.FAILED)
            .findFirst();
    if (failed.isPresent()) {
      copyProgressFields(dto, failed.get());
      dto.setWikiReferenceMigrationComplete(false);
      return;
    }
    if (migrationFullyComplete()) {
      dto.setWikiReferenceMigrationComplete(true);
      dto.setCurrentStepName(null);
      dto.setStepStatus(WikiReferenceMigrationStepStatus.COMPLETED.name());
      dto.setLastError(null);
      dto.setProcessedCount(aggregateProcessedWhenComplete());
      dto.setTotalCount(aggregateTotalWhenComplete());
      return;
    }
    dto.setWikiReferenceMigrationComplete(false);
    String activeStep = activeIncompleteStepName();
    dto.setCurrentStepName(activeStep);
    wikiReferenceMigrationProgressService
        .find(activeStep)
        .ifPresentOrElse(
            p -> copyProgressFields(dto, p),
            () -> {
              dto.setStepStatus(WikiReferenceMigrationStepStatus.PENDING.name());
              dto.setProcessedCount(0);
              dto.setTotalCount(estimatedPendingTotalForStep(activeStep));
              dto.setLastError(null);
            });
  }

  private int estimatedPendingTotalForStep(String activeStep) {
    if (STEP_RELATIONSHIP_WIKI_BACKFILL.equals(activeStep)) {
      return noteRepository.findRelationshipNotesForWikiReferenceMigrationOrderByIdAsc().size();
    }
    return noteRepository.findAllNonDeletedNotesOrderByIdAsc().size();
  }

  private static void copyProgressFields(
      AdminDataMigrationStatusDTO dto, WikiReferenceMigrationProgress p) {
    dto.setCurrentStepName(p.getStepName());
    dto.setStepStatus(p.getStatus().name());
    dto.setProcessedCount(p.getProcessedCount());
    dto.setTotalCount(p.getTotalCount());
    dto.setLastError(p.getLastError());
  }

  private int aggregateProcessedWhenComplete() {
    return List.of(STEP_RELATIONSHIP_WIKI_BACKFILL, STEP_NOTE_SLUG_PATH_REGENERATION).stream()
        .mapToInt(
            s ->
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getProcessedCount)
                    .orElse(0))
        .sum();
  }

  private int aggregateTotalWhenComplete() {
    return List.of(STEP_RELATIONSHIP_WIKI_BACKFILL, STEP_NOTE_SLUG_PATH_REGENERATION).stream()
        .mapToInt(
            s ->
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getTotalCount)
                    .orElse(0))
        .sum();
  }

  private String activeIncompleteStepName() {
    if (!stepCompleted(STEP_RELATIONSHIP_WIKI_BACKFILL)) {
      return STEP_RELATIONSHIP_WIKI_BACKFILL;
    }
    return STEP_NOTE_SLUG_PATH_REGENERATION;
  }

  private boolean migrationFullyComplete() {
    return stepCompleted(STEP_RELATIONSHIP_WIKI_BACKFILL)
        && stepCompleted(STEP_NOTE_SLUG_PATH_REGENERATION);
  }

  private boolean stepCompleted(String stepName) {
    return wikiReferenceMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }

  private Optional<String> findFailedStepName() {
    for (String s : List.of(STEP_RELATIONSHIP_WIKI_BACKFILL, STEP_NOTE_SLUG_PATH_REGENERATION)) {
      if (wikiReferenceMigrationProgressService
          .find(s)
          .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.FAILED)
          .orElse(false)) {
        return Optional.of(s);
      }
    }
    return Optional.empty();
  }
}
