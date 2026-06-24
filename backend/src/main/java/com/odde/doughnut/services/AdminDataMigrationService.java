package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.TitleAliasMigrationPreviewStatus;
import com.odde.doughnut.algorithms.TitleAliasMigrationTransform;
import com.odde.doughnut.controllers.dto.AdminDataMigrationDryRunDTO;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationNotePreviewDTO;
import com.odde.doughnut.entities.AdminDataMigrationProgress;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AdminDataMigrationService implements AdminDataMigrationProgressPopulator {

  public static final String DIAGNOSTIC_MARKER = "admin-data-migration-diagnostics:v1";

  public static final String STEP_TITLE_ALIAS_TO_FRONTMATTER = "title_alias_to_frontmatter";

  public static final String READY_MESSAGE =
      ("[%s]: Title alias to frontmatter migration runs in bounded batches.")
          .formatted(DIAGNOSTIC_MARKER);

  /**
   * Ordered steps that gate completion reporting and batch routing. Populate this list and
   * implement {@link AdminDataMigrationBatchWorker} when adding migrations.
   */
  public static final List<String> orderedAdminDataMigrationSteps =
      List.of(STEP_TITLE_ALIAS_TO_FRONTMATTER);

  /** Default notes per HTTP request once batch processing is wired for steps. */
  public static final int DATA_MIGRATION_BATCH_SIZE = 50;

  private final AdminDataMigrationProgressService adminDataMigrationProgressService;
  private final AdminDataMigrationBatchWorker batchWorker;
  private final NoteRepository noteRepository;

  public AdminDataMigrationService(
      AdminDataMigrationProgressService adminDataMigrationProgressService,
      @Lazy AdminDataMigrationBatchWorker batchWorker,
      NoteRepository noteRepository) {
    this.adminDataMigrationProgressService = adminDataMigrationProgressService;
    this.batchWorker = batchWorker;
    this.noteRepository = noteRepository;
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
      if (preview.status() == TitleAliasMigrationPreviewStatus.MIGRATE) {
        migrateCount++;
      }
    }
    dto.setTotalNoteCount(notes.size());
    dto.setMigrateCount(migrateCount);
    dto.setNoChangesCount(notes.size() - migrateCount);
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
      dto.setMessage("Title alias to frontmatter migration is already complete.");
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
      return;
    }
    if (migrationFullyComplete()) {
      dto.setDataMigrationComplete(true);
      dto.setCurrentStepName(null);
      dto.setStepStatus(WikiReferenceMigrationStepStatus.COMPLETED.name());
      dto.setLastError(null);
      dto.setProcessedCount(aggregateProcessedWhenComplete());
      dto.setTotalCount(aggregateTotalWhenComplete());
      return;
    }
    dto.setDataMigrationComplete(false);
    String activeStep = activeIncompleteStepName();
    dto.setCurrentStepName(activeStep);
    adminDataMigrationProgressService
        .find(activeStep)
        .ifPresentOrElse(
            p -> copyProgressFields(dto, p),
            () -> {
              dto.setStepStatus(WikiReferenceMigrationStepStatus.PENDING.name());
              dto.setProcessedCount(0);
              dto.setTotalCount(0);
              dto.setLastError(null);
            });
  }

  private static void copyProgressFields(
      AdminDataMigrationStatusDTO dto, AdminDataMigrationProgress p) {
    dto.setCurrentStepName(p.getStepName());
    dto.setStepStatus(p.getStatus().name());
    dto.setProcessedCount(p.getProcessedCount());
    dto.setTotalCount(p.getTotalCount());
    dto.setLastError(p.getLastError());
  }

  private int aggregateProcessedWhenComplete() {
    return orderedAdminDataMigrationSteps.stream()
        .mapToInt(
            s ->
                adminDataMigrationProgressService
                    .find(s)
                    .map(AdminDataMigrationProgress::getProcessedCount)
                    .orElse(0))
        .sum();
  }

  private int aggregateTotalWhenComplete() {
    return orderedAdminDataMigrationSteps.stream()
        .mapToInt(
            s ->
                adminDataMigrationProgressService
                    .find(s)
                    .map(AdminDataMigrationProgress::getTotalCount)
                    .orElse(0))
        .sum();
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
