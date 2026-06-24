package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.TitleAliasMigrationCollisionPolicy;
import com.odde.doughnut.algorithms.TitleAliasMigrationPreviewStatus;
import com.odde.doughnut.algorithms.TitleAliasMigrationTransform;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs one batch for the lowest incomplete step registered in {@link
 * AdminDataMigrationService#orderedAdminDataMigrationSteps}.
 */
@Service
public class AdminDataMigrationBatchWorker {

  private final AdminDataMigrationProgressService adminDataMigrationProgressService;
  private final AdminDataMigrationProgressPopulator progressPopulator;
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final WikiTitleCacheService wikiTitleCacheService;

  public AdminDataMigrationBatchWorker(
      AdminDataMigrationProgressService adminDataMigrationProgressService,
      @Lazy AdminDataMigrationProgressPopulator progressPopulator,
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      WikiTitleCacheService wikiTitleCacheService) {
    this.adminDataMigrationProgressService = adminDataMigrationProgressService;
    this.progressPopulator = progressPopulator;
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.wikiTitleCacheService = wikiTitleCacheService;
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminDataMigrationStatusDTO executeBatch(User adminUser) {
    if (AdminDataMigrationService.orderedAdminDataMigrationSteps.isEmpty()) {
      return noStepsConfiguredBatch();
    }
    if (stepCompleted(AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER)) {
      return migrationAlreadyCompleteBatch();
    }
    return runTitleAliasToFrontmatterBatch(adminUser);
  }

  private AdminDataMigrationStatusDTO runTitleAliasToFrontmatterBatch(User adminUser) {
    String step = AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER;
    List<Note> allNotes = noteRepository.findAllNonDeletedOrderByIdAsc();
    List<Integer> orderedIds = allNotes.stream().map(Note::getId).toList();
    List<TitleAliasMigrationCollisionPolicy.NotePlacement> placements =
        AdminDataMigrationService.titleAliasPlacementsFor(allNotes);
    Set<Integer> collisionNoteIds = TitleAliasMigrationCollisionPolicy.collisionNoteIds(placements);

    adminDataMigrationProgressService.startOrResume(step, allNotes.size());

    List<Integer> pendingIds =
        adminDataMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
    if (pendingIds.isEmpty()) {
      return finishWhenNoPendingNotes(step, allNotes, collisionNoteIds);
    }

    List<Integer> batchIds =
        pendingIds.stream().limit(AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE).toList();
    Map<Integer, Note> notesById =
        allNotes.stream().collect(Collectors.toMap(Note::getId, Function.identity()));

    int migratedCount = 0;
    int skippedCollisionCount = 0;
    for (Integer noteId : batchIds) {
      Note note = notesById.get(noteId);
      TitleAliasMigrationTransform.Preview preview =
          TitleAliasMigrationTransform.preview(note.getTitle(), note.getContent());
      if (preview.status() == TitleAliasMigrationPreviewStatus.MIGRATE
          && collisionNoteIds.contains(noteId)) {
        skippedCollisionCount++;
        continue;
      }
      if (preview.status() == TitleAliasMigrationPreviewStatus.MIGRATE) {
        note.setTitle(preview.plannedTitle());
        note.setContent(preview.plannedContent());
        entityPersister.merge(note);
        entityPersister.flush();
        wikiTitleCacheService.refreshForNote(note, adminUser);
        migratedCount++;
      }
    }

    int lastId = batchIds.getLast();
    adminDataMigrationProgressService.recordBatchSuccess(step, lastId, batchIds.size());

    List<Integer> stillPending =
        adminDataMigrationProgressService.pendingNoteIdsOrdered(step, orderedIds);
    if (stillPending.isEmpty()) {
      return finishWhenNoPendingNotes(step, allNotes, collisionNoteIds);
    }

    return batchResult(
        "title_alias_to_frontmatter: migrated %d note(s); skipped %d collision note(s) pending"
            + " disambiguation; processed %d note(s) in this batch."
                .formatted(migratedCount, skippedCollisionCount, batchIds.size()));
  }

  private AdminDataMigrationStatusDTO finishWhenNoPendingNotes(
      String step, List<Note> allNotes, Set<Integer> collisionNoteIds) {
    int pendingCollisionMigratables = countPendingCollisionMigratables(allNotes, collisionNoteIds);
    if (pendingCollisionMigratables > 0) {
      return batchResult(
          ("title_alias_to_frontmatter: simple migrations complete; %d collision note(s) pending"
                  + " disambiguation.")
              .formatted(pendingCollisionMigratables));
    }
    adminDataMigrationProgressService.markCompleted(step);
    return batchResult("title_alias_to_frontmatter migration is complete.");
  }

  private static int countPendingCollisionMigratables(
      List<Note> notes, Set<Integer> collisionNoteIds) {
    int count = 0;
    for (Note note : notes) {
      if (!collisionNoteIds.contains(note.getId())) {
        continue;
      }
      TitleAliasMigrationTransform.Preview preview =
          TitleAliasMigrationTransform.preview(note.getTitle(), note.getContent());
      if (preview.status() == TitleAliasMigrationPreviewStatus.MIGRATE) {
        count++;
      }
    }
    return count;
  }

  private AdminDataMigrationStatusDTO noStepsConfiguredBatch() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Batch acknowledged: no admin data migration steps are configured.");
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO migrationAlreadyCompleteBatch() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Title alias to frontmatter migration is already complete.");
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO batchResult(String message) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(message);
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private boolean stepCompleted(String stepName) {
    return adminDataMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }
}
