package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.TitleAliasInboundReferenceRewritePreview;
import com.odde.doughnut.algorithms.TitleAliasMigrationCollisionPolicy;
import com.odde.doughnut.algorithms.TitleAliasMigrationPreviewStatus;
import com.odde.doughnut.algorithms.TitleAliasMigrationTransform;
import com.odde.doughnut.controllers.dto.AdminDataMigrationDryRunDTO;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.controllers.dto.TitleAliasInboundReferenceRewritePreviewDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationCollisionGroupDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationCollisionMemberDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationNotePreviewDTO;
import com.odde.doughnut.entities.AdminDataMigrationProgress;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NoteTitlePlacement;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AdminDataMigrationService implements AdminDataMigrationProgressPopulator {

  public static final String DIAGNOSTIC_MARKER = "admin-data-migration-diagnostics:v1";

  public static final String STEP_TITLE_ALIAS_TO_FRONTMATTER = "title_alias_to_frontmatter";

  public static final String STEP_TITLE_ALIAS_INBOUND_REFERENCE_REWRITE =
      "title_alias_inbound_reference_rewrite";

  public static final String READY_MESSAGE =
      ("[%s]: Title alias to frontmatter migration runs in bounded batches.")
          .formatted(DIAGNOSTIC_MARKER);

  /**
   * Ordered steps that gate completion reporting and batch routing. Populate this list and
   * implement {@link AdminDataMigrationBatchWorker} when adding migrations.
   */
  public static final List<String> orderedAdminDataMigrationSteps =
      List.of(STEP_TITLE_ALIAS_TO_FRONTMATTER, STEP_TITLE_ALIAS_INBOUND_REFERENCE_REWRITE);

  /** Default notes per HTTP request once batch processing is wired for steps. */
  public static final int DATA_MIGRATION_BATCH_SIZE = 50;

  private final AdminDataMigrationProgressService adminDataMigrationProgressService;
  private final AdminDataMigrationBatchWorker batchWorker;
  private final NoteRepository noteRepository;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;

  public AdminDataMigrationService(
      AdminDataMigrationProgressService adminDataMigrationProgressService,
      @Lazy AdminDataMigrationBatchWorker batchWorker,
      NoteRepository noteRepository,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository) {
    this.adminDataMigrationProgressService = adminDataMigrationProgressService;
    this.batchWorker = batchWorker;
    this.noteRepository = noteRepository;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
  }

  public AdminDataMigrationStatusDTO getStatus() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(READY_MESSAGE);
    populateMigrationProgress(dto);
    return dto;
  }

  public AdminDataMigrationDryRunDTO dryRun() {
    List<Note> notes = noteRepository.findAllNonDeletedOrderByIdAsc();
    AdminDataMigrationDryRunDTO dto = new AdminDataMigrationDryRunDTO();
    int migrateCount = 0;
    List<TitleAliasMigrationCollisionPolicy.NotePlacement> placements =
        titleAliasPlacementsFor(
            notes.stream().map(AdminDataMigrationService::toPlacement).toList());
    Map<Integer, TitleAliasMigrationNotePreviewDTO> previewsByNoteId = new HashMap<>();
    for (Note note : notes) {
      TitleAliasMigrationTransform.Preview preview =
          TitleAliasMigrationTransform.preview(note.getTitle(), note.getContent());
      TitleAliasMigrationNotePreviewDTO item = new TitleAliasMigrationNotePreviewDTO();
      item.setNoteId(note.getId());
      item.setCurrentTitle(note.getTitle());
      item.setPlannedTitle(preview.plannedTitle());
      item.setPlannedAliases(preview.plannedAliases());
      item.setPlannedContent(preview.plannedContent());
      item.setStatus(preview.status().name());
      dto.getNotePreviews().add(item);
      previewsByNoteId.put(note.getId(), item);
      if (preview.status() == TitleAliasMigrationPreviewStatus.MIGRATE) {
        migrateCount++;
      }
    }
    applyCollisionResolution(dto, placements, previewsByNoteId);
    dto.setTotalNoteCount(notes.size());
    dto.setMigrateCount(migrateCount);
    dto.setNoChangesCount(notes.size() - migrateCount);
    populateInboundReferenceRewritePreview(dto);
    return dto;
  }

  private static void applyCollisionResolution(
      AdminDataMigrationDryRunDTO dto,
      List<TitleAliasMigrationCollisionPolicy.NotePlacement> placements,
      Map<Integer, TitleAliasMigrationNotePreviewDTO> previewsByNoteId) {
    Map<Integer, String> resolvedTitles = TitleAliasMigrationCollisionPolicy.resolve(placements);
    resolvedTitles.forEach(
        (noteId, resolvedTitle) -> {
          TitleAliasMigrationNotePreviewDTO preview = previewsByNoteId.get(noteId);
          if (preview != null) {
            preview.setPlannedTitle(resolvedTitle);
          }
        });
    List<TitleAliasMigrationCollisionPolicy.CollisionGroup> groups =
        TitleAliasMigrationCollisionPolicy.collisionGroups(placements);
    int collisionNoteCount = 0;
    for (TitleAliasMigrationCollisionPolicy.CollisionGroup group : groups) {
      TitleAliasMigrationCollisionGroupDTO groupDto = new TitleAliasMigrationCollisionGroupDTO();
      groupDto.setNotebookId(group.notebookId());
      groupDto.setFolderId(group.folderId());
      groupDto.setBasePlannedTitle(group.basePlannedTitle());
      for (TitleAliasMigrationCollisionPolicy.Member member : group.members()) {
        TitleAliasMigrationCollisionMemberDTO memberDto =
            new TitleAliasMigrationCollisionMemberDTO();
        memberDto.setNoteId(member.noteId());
        memberDto.setResolvedTitle(member.resolvedTitle());
        groupDto.getMembers().add(memberDto);
      }
      dto.getCollisionGroups().add(groupDto);
      collisionNoteCount += group.members().size();
    }
    dto.setCollisionGroupCount(groups.size());
    dto.setCollisionNoteCount(collisionNoteCount);
  }

  private void populateInboundReferenceRewritePreview(AdminDataMigrationDryRunDTO dto) {
    if (!stepCompleted(STEP_TITLE_ALIAS_TO_FRONTMATTER)) {
      dto.setInboundReferenceRewriteCount(0);
      return;
    }
    List<TitleAliasInboundReferenceRewritePreviewDTO> previews = inboundReferenceRewritePreviews();
    dto.setInboundReferenceRewritePreviews(previews);
    dto.setInboundReferenceRewriteCount(previews.size());
  }

  private List<TitleAliasInboundReferenceRewritePreviewDTO> inboundReferenceRewritePreviews() {
    List<TitleAliasInboundReferenceRewritePreviewDTO> previews = new ArrayList<>();
    for (TitleAliasInboundReferenceRewritePreview.Item item :
        TitleAliasInboundReferenceRewritePreview.previewRows(
            noteWikiTitleCacheRepository.findAllRowsBetweenNonDeletedNotes())) {
      previews.add(toInboundReferenceRewritePreviewDto(item));
    }
    return previews;
  }

  List<Integer> targetNoteIdsNeedingInboundReferenceRewrite() {
    return TitleAliasInboundReferenceRewritePreview.targetNoteIdsWithPendingRewrites(
        noteWikiTitleCacheRepository.findAllRowsBetweenNonDeletedNotes());
  }

  private int countPendingInboundReferenceRewrites() {
    if (!stepCompleted(STEP_TITLE_ALIAS_TO_FRONTMATTER)) {
      return 0;
    }
    return inboundReferenceRewritePreviews().size();
  }

  private static TitleAliasInboundReferenceRewritePreviewDTO toInboundReferenceRewritePreviewDto(
      TitleAliasInboundReferenceRewritePreview.Item item) {
    TitleAliasInboundReferenceRewritePreviewDTO dto =
        new TitleAliasInboundReferenceRewritePreviewDTO();
    dto.setReferrerNoteId(item.referrerNoteId());
    dto.setTargetNoteId(item.targetNoteId());
    dto.setCurrentLinkInner(item.currentLinkInner());
    dto.setPlannedLinkInner(item.plannedLinkInner());
    dto.setVisibleTextWillChange(item.visibleTextWillChange());
    return dto;
  }

  public AdminDataMigrationStatusDTO runBatch(User adminUser) {
    Optional<String> failedStep = findFailedStepName();
    if (failedStep.isPresent()) {
      return dtoAfterBlockedRun(
          "Migration is stopped after a failure; fix data and clear progress if needed.");
    }
    if (migrationFullyComplete()) {
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage("Frontmatter alias migration is already complete.");
      populateMigrationProgress(dto);
      return dto;
    }
    try {
      return batchWorker.executeBatch(adminUser);
    } catch (RuntimeException e) {
      Optional<String> step = firstIncompleteTrackedStepName();
      if (step.isPresent()) {
        String failureMessage = failureMessage(step.get(), e);
        adminDataMigrationProgressService.markFailed(step.get(), errorMessageSafe(failureMessage));
        return dtoAfterFailure(failureMessage);
      }
      throw e;
    }
  }

  static List<TitleAliasMigrationCollisionPolicy.NotePlacement> titleAliasPlacementsFor(
      List<NoteTitlePlacement> notes) {
    List<TitleAliasMigrationCollisionPolicy.NotePlacement> placements = new ArrayList<>();
    for (NoteTitlePlacement note : notes) {
      placements.add(notePlacement(note));
    }
    return List.copyOf(placements);
  }

  static int countNotesPendingTitleAliasMigration(List<NoteTitlePlacement> notes) {
    int pending = 0;
    for (NoteTitlePlacement note : notes) {
      if (needsTitleAliasMigration(note)) {
        pending++;
      }
    }
    return pending;
  }

  static boolean needsTitleAliasMigration(NoteTitlePlacement note) {
    return TitleAliasMigrationTransform.statusFor(note.title())
        == TitleAliasMigrationPreviewStatus.MIGRATE;
  }

  /**
   * Rebuilds collision-group placements with stable base titles so partial collision batches keep
   * dry-run disambiguation indices.
   */
  static List<TitleAliasMigrationCollisionPolicy.NotePlacement> collisionResolutionPlacementsFor(
      List<NoteTitlePlacement> notes) {
    Map<String, String> baseByScopeKey = new LinkedHashMap<>();
    for (NoteTitlePlacement note : notes) {
      if (TitleAliasMigrationTransform.statusFor(note.title())
          != TitleAliasMigrationPreviewStatus.MIGRATE) {
        continue;
      }
      TitleAliasMigrationCollisionPolicy.NotePlacement placement = notePlacement(note);
      baseByScopeKey.putIfAbsent(collisionScopeKey(placement), placement.basePlannedTitle());
    }
    if (baseByScopeKey.isEmpty()) {
      return List.of();
    }
    Map<Integer, TitleAliasMigrationCollisionPolicy.NotePlacement> byNoteId = new LinkedHashMap<>();
    int maxCollisionIndex = notes.size();
    for (NoteTitlePlacement note : notes) {
      int notebookId = note.notebookId();
      Integer folderId = note.folderId();
      TitleAliasMigrationPreviewStatus status =
          TitleAliasMigrationTransform.statusFor(note.title());
      String plannedTitle = TitleAliasMigrationTransform.plannedTitleFor(note.title());
      for (Map.Entry<String, String> scope : baseByScopeKey.entrySet()) {
        CollisionScope collisionScope = CollisionScope.parse(scope.getKey());
        if (collisionScope.notebookId() != notebookId
            || !Objects.equals(collisionScope.folderId(), folderId)) {
          continue;
        }
        String basePlannedTitle = scope.getValue();
        boolean member =
            status == TitleAliasMigrationPreviewStatus.MIGRATE
                ? plannedTitle.equals(basePlannedTitle)
                : TitleAliasMigrationCollisionPolicy.memberTitleMatchesCollisionBase(
                    note.title(), basePlannedTitle, maxCollisionIndex);
        if (member) {
          byNoteId.putIfAbsent(
              note.id(),
              new TitleAliasMigrationCollisionPolicy.NotePlacement(
                  note.id(), notebookId, folderId, basePlannedTitle));
        }
      }
    }
    return List.copyOf(byNoteId.values());
  }

  static NoteTitlePlacement toPlacement(Note note) {
    return new NoteTitlePlacement(
        note.getId(),
        note.getTitle(),
        note.getNotebook().getId(),
        note.getFolder() != null ? note.getFolder().getId() : null);
  }

  private static TitleAliasMigrationCollisionPolicy.NotePlacement notePlacement(
      NoteTitlePlacement note) {
    return new TitleAliasMigrationCollisionPolicy.NotePlacement(
        note.id(),
        note.notebookId(),
        note.folderId(),
        TitleAliasMigrationTransform.plannedTitleFor(note.title()));
  }

  private record CollisionScope(int notebookId, Integer folderId) {
    static CollisionScope parse(String key) {
      String[] parts = key.split(":", 3);
      return new CollisionScope(
          Integer.parseInt(parts[0]), "null".equals(parts[1]) ? null : Integer.valueOf(parts[1]));
    }
  }

  private static String collisionScopeKey(
      TitleAliasMigrationCollisionPolicy.NotePlacement placement) {
    return placement.notebookId() + ":" + placement.folderId() + ":" + placement.basePlannedTitle();
  }

  private static String errorMessageSafe(RuntimeException e) {
    return errorMessageSafe(e.getMessage());
  }

  private static String errorMessageSafe(String m) {
    if (m != null && m.length() > 65535) {
      return m.substring(0, 65535);
    }
    return m != null ? m : "";
  }

  @Override
  public void populateMigrationProgress(AdminDataMigrationStatusDTO dto) {
    if (orderedAdminDataMigrationSteps.isEmpty()) {
      dto.setDataMigrationComplete(true);
      dto.setCurrentStepName(null);
      dto.setStepStatus(WikiReferenceMigrationStepStatus.COMPLETED.name());
      dto.setProcessedCount(0);
      dto.setTotalCount(0);
      dto.setLastError(null);
      dto.setPendingInboundReferenceRewriteCount(0);
      return;
    }
    Optional<AdminDataMigrationProgress> failed =
        orderedAdminDataMigrationSteps.stream()
            .flatMap(s -> adminDataMigrationProgressService.find(s).stream())
            .filter(p -> p.getStatus() == WikiReferenceMigrationStepStatus.FAILED)
            .findFirst();
    if (failed.isPresent()) {
      copyProgressFields(dto, failed.get());
      dto.setDataMigrationComplete(false);
      dto.setPendingInboundReferenceRewriteCount(0);
      return;
    }
    if (migrationFullyComplete()) {
      dto.setDataMigrationComplete(true);
      dto.setCurrentStepName(null);
      dto.setStepStatus(WikiReferenceMigrationStepStatus.COMPLETED.name());
      dto.setLastError(null);
      applyTitleAliasMigrationProgressCounts(dto);
      dto.setPendingInboundReferenceRewriteCount(0);
      return;
    }
    dto.setDataMigrationComplete(false);
    String activeStep = activeIncompleteStepName();
    dto.setCurrentStepName(activeStep);
    adminDataMigrationProgressService
        .find(activeStep)
        .ifPresentOrElse(
            p -> {
              copyProgressFields(dto, p);
              applyProgressCountsForStep(dto, activeStep);
            },
            () -> {
              dto.setStepStatus(WikiReferenceMigrationStepStatus.PENDING.name());
              dto.setProcessedCount(0);
              dto.setTotalCount(0);
              dto.setLastError(null);
              applyProgressCountsForStep(dto, activeStep);
            });
    if (!STEP_TITLE_ALIAS_INBOUND_REFERENCE_REWRITE.equals(activeStep)) {
      dto.setPendingInboundReferenceRewriteCount(0);
    } else {
      dto.setPendingInboundReferenceRewriteCount(countPendingInboundReferenceRewrites());
    }
  }

  private void applyProgressCountsForStep(AdminDataMigrationStatusDTO dto, String activeStep) {
    if (STEP_TITLE_ALIAS_TO_FRONTMATTER.equals(activeStep)) {
      applyTitleAliasMigrationProgressCounts(dto);
      return;
    }
    if (STEP_TITLE_ALIAS_INBOUND_REFERENCE_REWRITE.equals(activeStep)) {
      applyInboundReferenceRewriteProgressCounts(dto);
    }
  }

  private void applyInboundReferenceRewriteProgressCounts(AdminDataMigrationStatusDTO dto) {
    List<Integer> pendingTargets = targetNoteIdsNeedingInboundReferenceRewrite();
    int pending = pendingTargets.size();
    adminDataMigrationProgressService
        .find(STEP_TITLE_ALIAS_INBOUND_REFERENCE_REWRITE)
        .ifPresentOrElse(
            p -> {
              int total =
                  p.getTotalCount() > 0 ? p.getTotalCount() : pending + p.getProcessedCount();
              dto.setTotalCount(total);
              dto.setProcessedCount(Math.max(0, total - pending));
            },
            () -> {
              dto.setTotalCount(pending);
              dto.setProcessedCount(0);
            });
  }

  private void applyTitleAliasMigrationProgressCounts(AdminDataMigrationStatusDTO dto) {
    long pending = noteRepository.countNonDeletedNotesWithPlainTitleAlias();
    long total = noteRepository.countNonDeleted();
    dto.setTotalCount((int) total);
    dto.setProcessedCount((int) (total - pending));
  }

  private static void copyProgressFields(
      AdminDataMigrationStatusDTO dto, AdminDataMigrationProgress p) {
    dto.setCurrentStepName(p.getStepName());
    dto.setStepStatus(p.getStatus().name());
    dto.setProcessedCount(p.getProcessedCount());
    dto.setTotalCount(p.getTotalCount());
    dto.setLastError(p.getLastError());
  }

  private String activeIncompleteStepName() {
    for (String step : orderedAdminDataMigrationSteps) {
      if (!stepCompleted(step)) {
        return step;
      }
    }
    throw new IllegalStateException("No incomplete tracked migration step");
  }

  private boolean migrationFullyComplete() {
    return orderedAdminDataMigrationSteps.stream().allMatch(this::stepCompleted);
  }

  private boolean stepCompleted(String stepName) {
    return adminDataMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }

  private Optional<String> firstIncompleteTrackedStepName() {
    for (String step : orderedAdminDataMigrationSteps) {
      if (!stepCompleted(step)) {
        return Optional.of(step);
      }
    }
    return Optional.empty();
  }

  private AdminDataMigrationStatusDTO dtoAfterBlockedRun(String message) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(message);
    populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO dtoAfterFailure(String message) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Migration batch failed: " + message);
    populateMigrationProgress(dto);
    return dto;
  }

  private static String failureMessage(String step, RuntimeException e) {
    return "marker=%s; step=%s; batchSize=%d; cause=%s; rootCause=%s"
        .formatted(
            DIAGNOSTIC_MARKER,
            step,
            DATA_MIGRATION_BATCH_SIZE,
            e.getMessage() == null ? "" : e.getMessage(),
            rootCauseMessage(e));
  }

  private static String rootCauseMessage(Throwable e) {
    Throwable root = e;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    return root.getMessage() == null ? "" : root.getMessage();
  }

  private Optional<String> findFailedStepName() {
    for (String s : orderedAdminDataMigrationSteps) {
      if (adminDataMigrationProgressService
          .find(s)
          .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.FAILED)
          .orElse(false)) {
        return Optional.of(s);
      }
    }
    return Optional.empty();
  }
}
