package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AdminDataMigrationService implements AdminDataMigrationProgressPopulator {

  public static final String DIAGNOSTIC_MARKER = "admin-data-migration-diagnostics:v1";

  public static final String READY_MESSAGE =
      ("[%s]: Admin-triggered migration runs in bounded batches; progress rows live in "
              + "wiki_reference_migration_progress keyed by step name.")
          .formatted(DIAGNOSTIC_MARKER);

  /**
   * Ordered steps that gate completion reporting and batch routing. Populate this list and
   * implement {@link AdminDataMigrationBatchWorker} when adding migrations.
   */
  public static final List<String> orderedAdminDataMigrationSteps = List.of();

  /** Default notes per HTTP request once batch processing is wired for steps. */
  public static final int DATA_MIGRATION_BATCH_SIZE = 50;

  private final WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService;
  private final AdminDataMigrationBatchWorker batchWorker;

  public AdminDataMigrationService(
      WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService,
      @Lazy AdminDataMigrationBatchWorker batchWorker) {
    this.wikiReferenceMigrationProgressService = wikiReferenceMigrationProgressService;
    this.batchWorker = batchWorker;
  }

  public AdminDataMigrationStatusDTO getStatus() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(READY_MESSAGE);
    populateMigrationProgress(dto);
    return dto;
  }

  public AdminDataMigrationStatusDTO runBatch(User adminUser) {
    Optional<String> failedStep = findFailedStepName();
    if (failedStep.isPresent()) {
      return dtoAfterBlockedRun(
          "Migration is stopped after a failure; fix data and clear progress if needed.");
    }
    try {
      return batchWorker.executeBatch(adminUser);
    } catch (RuntimeException e) {
      Optional<String> step = firstIncompleteTrackedStepName();
      if (step.isPresent()) {
        String failureMessage = failureMessage(step.get(), e);
        wikiReferenceMigrationProgressService.markFailed(
            step.get(), errorMessageSafe(failureMessage));
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
    Optional<WikiReferenceMigrationProgress> failed =
        orderedAdminDataMigrationSteps.stream()
            .flatMap(s -> wikiReferenceMigrationProgressService.find(s).stream())
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
    wikiReferenceMigrationProgressService
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
      AdminDataMigrationStatusDTO dto, WikiReferenceMigrationProgress p) {
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
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getProcessedCount)
                    .orElse(0))
        .sum();
  }

  private int aggregateTotalWhenComplete() {
    return orderedAdminDataMigrationSteps.stream()
        .mapToInt(
            s ->
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getTotalCount)
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
    return wikiReferenceMigrationProgressService
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
