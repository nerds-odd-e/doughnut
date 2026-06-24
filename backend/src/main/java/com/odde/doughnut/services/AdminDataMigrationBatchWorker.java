package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.TitleAliasInboundReferenceRewritePreview;
import com.odde.doughnut.algorithms.TitleAliasMigrationCollisionPolicy;
import com.odde.doughnut.algorithms.TitleAliasMigrationPreviewStatus;
import com.odde.doughnut.algorithms.TitleAliasMigrationTransform;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
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
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final EntityPersister entityPersister;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final WikiLinkRewriteService wikiLinkRewriteService;

  public AdminDataMigrationBatchWorker(
      AdminDataMigrationProgressService adminDataMigrationProgressService,
      @Lazy AdminDataMigrationProgressPopulator progressPopulator,
      NoteRepository noteRepository,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      EntityPersister entityPersister,
      WikiTitleCacheService wikiTitleCacheService,
      WikiLinkRewriteService wikiLinkRewriteService) {
    this.adminDataMigrationProgressService = adminDataMigrationProgressService;
    this.progressPopulator = progressPopulator;
    this.noteRepository = noteRepository;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.entityPersister = entityPersister;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminDataMigrationStatusDTO executeBatch(User adminUser) {
    if (AdminDataMigrationService.orderedAdminDataMigrationSteps.isEmpty()) {
      return noStepsConfiguredBatch();
    }
    if (migrationFullyComplete()) {
      return migrationAlreadyCompleteBatch();
    }
    if (!stepCompleted(AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER)) {
      return runTitleAliasToFrontmatterBatch(adminUser);
    }
    return runInboundReferenceRewriteBatch(adminUser);
  }

  private AdminDataMigrationStatusDTO runInboundReferenceRewriteBatch(User adminUser) {
    String step = AdminDataMigrationService.STEP_TITLE_ALIAS_INBOUND_REFERENCE_REWRITE;
    List<Integer> orderedTargetIds = targetNoteIdsNeedingInboundReferenceRewrite();

    if (orderedTargetIds.isEmpty()) {
      adminDataMigrationProgressService.startOrResume(step, 0);
      adminDataMigrationProgressService.markCompleted(step);
      return batchResult("title_alias_inbound_reference_rewrite migration is complete.");
    }

    adminDataMigrationProgressService.startOrResume(step, orderedTargetIds.size());

    List<Integer> pendingIds =
        adminDataMigrationProgressService.pendingNoteIdsOrdered(step, orderedTargetIds);
    if (pendingIds.isEmpty()) {
      adminDataMigrationProgressService.markCompleted(step);
      return batchResult("title_alias_inbound_reference_rewrite migration is complete.");
    }

    List<Integer> batchIds =
        pendingIds.stream().limit(AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE).toList();
    Map<Integer, Note> notesById = new java.util.HashMap<>();
    for (Note note : noteRepository.findAllNonDeletedOrderByIdAsc()) {
      if (batchIds.contains(note.getId())) {
        notesById.put(note.getId(), note);
      }
    }
    Timestamp updatedAt = new Timestamp(System.currentTimeMillis());

    int rewrittenTargetCount = 0;
    for (Integer targetId : batchIds) {
      Note target = notesById.get(targetId);
      if (target == null || target.getDeletedAt() != null) {
        continue;
      }
      wikiLinkRewriteService.rewriteInboundWikiLinksForTitleAliasMigration(
          target, updatedAt, adminUser);
      rewrittenTargetCount++;
    }

    int lastId = batchIds.getLast();
    adminDataMigrationProgressService.recordBatchSuccess(step, lastId, batchIds.size());

    List<Integer> stillPending = targetNoteIdsNeedingInboundReferenceRewrite();
    if (stillPending.isEmpty()) {
      adminDataMigrationProgressService.markCompleted(step);
      return batchResult("title_alias_inbound_reference_rewrite migration is complete.");
    }

    return batchResult(
        ("title_alias_inbound_reference_rewrite: rewrote inbound links for %d target note(s);"
                + " %d target note(s) remaining.")
            .formatted(rewrittenTargetCount, stillPending.size()));
  }

  private List<Integer> targetNoteIdsNeedingInboundReferenceRewrite() {
    return TitleAliasInboundReferenceRewritePreview.targetNoteIdsWithPendingRewrites(
        noteWikiTitleCacheRepository.findAllRowsBetweenNonDeletedNotes());
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
      List<Integer> pendingCollisionIds = pendingCollisionMigratableIds(allNotes, collisionNoteIds);
      if (!pendingCollisionIds.isEmpty()) {
        return runCollisionDisambiguationBatch(adminUser, step, allNotes, pendingCollisionIds);
      }
      if (AdminDataMigrationService.countNotesPendingTitleAliasMigration(allNotes) == 0) {
        adminDataMigrationProgressService.markCompleted(step);
      }
      return batchResult("title_alias_to_frontmatter migration is complete.");
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
        applyTitleAliasMigration(note, adminUser, preview.plannedTitle(), preview.plannedContent());
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

  private AdminDataMigrationStatusDTO runCollisionDisambiguationBatch(
      User adminUser, String step, List<Note> allNotes, List<Integer> pendingCollisionIds) {
    List<Integer> batchIds =
        pendingCollisionIds.stream()
            .limit(AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE)
            .toList();
    List<TitleAliasMigrationCollisionPolicy.NotePlacement> resolutionPlacements =
        AdminDataMigrationService.collisionResolutionPlacementsFor(allNotes);
    Map<Integer, String> resolvedTitles =
        TitleAliasMigrationCollisionPolicy.resolve(resolutionPlacements);
    Map<Integer, Note> notesById =
        allNotes.stream().collect(Collectors.toMap(Note::getId, Function.identity()));

    int migratedCount = 0;
    for (Integer noteId : batchIds) {
      Note note = notesById.get(noteId);
      TitleAliasMigrationTransform.Preview preview =
          TitleAliasMigrationTransform.preview(note.getTitle(), note.getContent());
      if (preview.status() != TitleAliasMigrationPreviewStatus.MIGRATE) {
        continue;
      }
      String resolvedTitle = resolvedTitles.get(noteId);
      if (resolvedTitle == null) {
        continue;
      }
      applyTitleAliasMigration(note, adminUser, resolvedTitle, preview.plannedContent());
      migratedCount++;
    }

    List<Integer> stillPending =
        pendingCollisionMigratableIds(
            allNotes,
            TitleAliasMigrationCollisionPolicy.collisionNoteIds(
                AdminDataMigrationService.collisionResolutionPlacementsFor(allNotes)));
    if (stillPending.isEmpty()
        && AdminDataMigrationService.countNotesPendingTitleAliasMigration(allNotes) == 0) {
      adminDataMigrationProgressService.markCompleted(step);
      return batchResult("title_alias_to_frontmatter migration is complete.");
    }

    return batchResult(
        ("title_alias_to_frontmatter: migrated %d collision note(s); %d collision note(s)"
                + " remaining.")
            .formatted(migratedCount, stillPending.size()));
  }

  private void applyTitleAliasMigration(Note note, User adminUser, String title, String content) {
    TitleAliasMigrationTransform.Preview preview =
        TitleAliasMigrationTransform.preview(note.getTitle(), note.getContent());
    if (preview.status() != TitleAliasMigrationPreviewStatus.MIGRATE) {
      return;
    }
    if (title.equals(note.getTitle()) && content.equals(note.getContent())) {
      return;
    }
    note.setTitle(title);
    note.setContent(content);
    entityPersister.merge(note);
    entityPersister.flush();
    wikiTitleCacheService.refreshForNote(note, adminUser);
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
    if (AdminDataMigrationService.countNotesPendingTitleAliasMigration(allNotes) == 0) {
      adminDataMigrationProgressService.markCompleted(step);
    }
    return batchResult("title_alias_to_frontmatter migration is complete.");
  }

  private static List<Integer> pendingCollisionMigratableIds(
      List<Note> notes, Set<Integer> collisionNoteIds) {
    return notes.stream()
        .filter(note -> collisionNoteIds.contains(note.getId()))
        .filter(AdminDataMigrationService::noteNeedsTitleAliasMigration)
        .map(Note::getId)
        .toList();
  }

  private static int countPendingCollisionMigratables(
      List<Note> notes, Set<Integer> collisionNoteIds) {
    return (int)
        notes.stream()
            .filter(note -> collisionNoteIds.contains(note.getId()))
            .filter(AdminDataMigrationService::noteNeedsTitleAliasMigration)
            .count();
  }

  private AdminDataMigrationStatusDTO noStepsConfiguredBatch() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Batch acknowledged: no admin data migration steps are configured.");
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO migrationAlreadyCompleteBatch() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Frontmatter alias migration is already complete.");
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO batchResult(String message) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(message);
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private boolean migrationFullyComplete() {
    return AdminDataMigrationService.orderedAdminDataMigrationSteps.stream()
        .allMatch(this::stepCompleted);
  }

  private boolean stepCompleted(String stepName) {
    return adminDataMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }
}
