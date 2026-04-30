package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
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
      "Wiki data migration: relationship title and details backfill, then note slug regeneration.";

  public static final String STEP_RELATIONSHIP_TITLE_BACKFILL = "relationship_title_backfill";
  public static final String STEP_RELATIONSHIP_DETAILS_BACKFILL = "relationship_details_backfill";
  public static final String STEP_NOTE_SLUG_PATH_REGENERATION = "note_slug_path_regeneration";

  /** Max relationship notes processed per HTTP request for title or details steps. */
  public static final int WIKI_REFERENCE_MIGRATION_BATCH_SIZE = 50;

  private final NoteRepository noteRepository;
  private final WikiSlugPathService wikiSlugPathService;
  private final EntityPersister entityPersister;
  private final WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService;

  public AdminDataMigrationService(
      NoteRepository noteRepository,
      WikiSlugPathService wikiSlugPathService,
      EntityPersister entityPersister,
      WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService) {
    this.noteRepository = noteRepository;
    this.wikiSlugPathService = wikiSlugPathService;
    this.entityPersister = entityPersister;
    this.wikiReferenceMigrationProgressService = wikiReferenceMigrationProgressService;
  }

  public AdminDataMigrationStatusDTO getStatus() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(READY_MESSAGE);
    populateMigrationProgress(dto);
    return dto;
  }

  @Transactional
  public AdminDataMigrationStatusDTO runBatch() {
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
    if (!stepCompleted(STEP_RELATIONSHIP_TITLE_BACKFILL)) {
      return runTitleBackfillBatch();
    }
    if (!stepCompleted(STEP_RELATIONSHIP_DETAILS_BACKFILL)) {
      return runDetailsBackfillBatch();
    }
    return runSlugRegenerationBatch();
  }

  private AdminDataMigrationStatusDTO runTitleBackfillBatch() {
    String step = STEP_RELATIONSHIP_TITLE_BACKFILL;
    try {
      List<Note> candidates = noteRepository.findRelationshipNotesWithBlankTitleForMigration();
      List<Integer> orderedIds = candidates.stream().map(Note::getId).toList();
      wikiReferenceMigrationProgressService.startOrResume(step, orderedIds.size());
      List<Integer> pending =
          wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
      if (pending.isEmpty()) {
        wikiReferenceMigrationProgressService.markCompleted(step);
        AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
        dto.setMessage("Title backfill: nothing pending.");
        populateMigrationProgress(dto);
        return dto;
      }
      int take = Math.min(WIKI_REFERENCE_MIGRATION_BATCH_SIZE, pending.size());
      List<Integer> batch = pending.subList(0, take);
      Map<Integer, Note> byId =
          candidates.stream().collect(Collectors.toMap(Note::getId, Function.identity()));
      for (Integer id : batch) {
        Note relation = byId.get(id);
        relation.setTitle(
            RelationshipNoteTitleFormatter.format(
                relation.getParent().getTitle(),
                relation.getRelationType().label,
                relation.getTargetNote().getTitle()));
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
      dto.setMessage("Title backfill: processed %d note(s) in this batch.".formatted(batch.size()));
      populateMigrationProgress(dto);
      return dto;
    } catch (RuntimeException e) {
      wikiReferenceMigrationProgressService.markFailed(step, e.getMessage());
      return dtoAfterFailure(step, e.getMessage());
    }
  }

  private AdminDataMigrationStatusDTO runDetailsBackfillBatch() {
    String step = STEP_RELATIONSHIP_DETAILS_BACKFILL;
    try {
      List<Note> candidates = noteRepository.findRelationshipNotesNeedingDetailsMigration();
      List<Integer> orderedIds = candidates.stream().map(Note::getId).toList();
      wikiReferenceMigrationProgressService.startOrResume(step, orderedIds.size());
      List<Integer> pending =
          wikiReferenceMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
      if (pending.isEmpty()) {
        wikiReferenceMigrationProgressService.markCompleted(step);
        AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
        dto.setMessage("Details backfill: nothing pending.");
        populateMigrationProgress(dto);
        return dto;
      }
      int take = Math.min(WIKI_REFERENCE_MIGRATION_BATCH_SIZE, pending.size());
      List<Integer> batch = pending.subList(0, take);
      Map<Integer, Note> byId =
          candidates.stream().collect(Collectors.toMap(Note::getId, Function.identity()));
      for (Integer id : batch) {
        Note relation = byId.get(id);
        relation.setDetails(
            RelationshipNoteMarkdownFormatter.format(
                relation.getRelationType(),
                relation.getParent().getTitle(),
                relation.getTargetNote().getTitle(),
                relation.getDetails()));
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
          "Details backfill: processed %d note(s) in this batch.".formatted(batch.size()));
      populateMigrationProgress(dto);
      return dto;
    } catch (RuntimeException e) {
      wikiReferenceMigrationProgressService.markFailed(step, e.getMessage());
      return dtoAfterFailure(step, e.getMessage());
    }
  }

  private AdminDataMigrationStatusDTO runSlugRegenerationBatch() {
    String step = STEP_NOTE_SLUG_PATH_REGENERATION;
    try {
      List<Note> allNotes = noteRepository.findAllNonDeletedNotesOrderByNotebookFolderAndId();
      int n = allNotes.size();
      wikiReferenceMigrationProgressService.startOrResume(step, n);
      wikiSlugPathService.regenerateAllNoteSlugPaths();
      entityPersister.flush();
      if (n > 0) {
        wikiReferenceMigrationProgressService.recordBatchSuccess(
            step, allNotes.get(n - 1).getId(), n);
      }
      wikiReferenceMigrationProgressService.markCompleted(step);
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage("Regenerated slug paths for all non-deleted notes.");
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
        List.of(
                STEP_RELATIONSHIP_TITLE_BACKFILL,
                STEP_RELATIONSHIP_DETAILS_BACKFILL,
                STEP_NOTE_SLUG_PATH_REGENERATION)
            .stream()
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
    if (STEP_RELATIONSHIP_TITLE_BACKFILL.equals(activeStep)) {
      return noteRepository.findRelationshipNotesWithBlankTitleForMigration().size();
    }
    if (STEP_RELATIONSHIP_DETAILS_BACKFILL.equals(activeStep)) {
      return noteRepository.findRelationshipNotesNeedingDetailsMigration().size();
    }
    return noteRepository.findAllNonDeletedNotesOrderByNotebookFolderAndId().size();
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
    return List.of(
            STEP_RELATIONSHIP_TITLE_BACKFILL,
            STEP_RELATIONSHIP_DETAILS_BACKFILL,
            STEP_NOTE_SLUG_PATH_REGENERATION)
        .stream()
        .mapToInt(
            s ->
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getProcessedCount)
                    .orElse(0))
        .sum();
  }

  private int aggregateTotalWhenComplete() {
    return List.of(
            STEP_RELATIONSHIP_TITLE_BACKFILL,
            STEP_RELATIONSHIP_DETAILS_BACKFILL,
            STEP_NOTE_SLUG_PATH_REGENERATION)
        .stream()
        .mapToInt(
            s ->
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getTotalCount)
                    .orElse(0))
        .sum();
  }

  private String activeIncompleteStepName() {
    if (!stepCompleted(STEP_RELATIONSHIP_TITLE_BACKFILL)) {
      return STEP_RELATIONSHIP_TITLE_BACKFILL;
    }
    if (!stepCompleted(STEP_RELATIONSHIP_DETAILS_BACKFILL)) {
      return STEP_RELATIONSHIP_DETAILS_BACKFILL;
    }
    return STEP_NOTE_SLUG_PATH_REGENERATION;
  }

  private boolean migrationFullyComplete() {
    return stepCompleted(STEP_RELATIONSHIP_TITLE_BACKFILL)
        && stepCompleted(STEP_RELATIONSHIP_DETAILS_BACKFILL)
        && stepCompleted(STEP_NOTE_SLUG_PATH_REGENERATION);
  }

  private boolean stepCompleted(String stepName) {
    return wikiReferenceMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }

  private Optional<String> findFailedStepName() {
    for (String s :
        List.of(
            STEP_RELATIONSHIP_TITLE_BACKFILL,
            STEP_RELATIONSHIP_DETAILS_BACKFILL,
            STEP_NOTE_SLUG_PATH_REGENERATION)) {
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
