package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminDataMigrationService implements AdminDataMigrationProgressPopulator {

  public static final String READY_MESSAGE =
      "Wiki data migration: relationship wiki backfill (title, details, cache), legacy parent"
          + " frontmatter and cache on child notes, then batched note slug regeneration.";

  public static final String STEP_RELATIONSHIP_WIKI_BACKFILL = "relationship_wiki_backfill";
  public static final String STEP_LEGACY_PARENT_FRONTMATTER = "legacy_parent_frontmatter";
  public static final String STEP_NOTE_SLUG_PATH_REGENERATION = "note_slug_path_regeneration";

  private static final List<String> WIKI_REFERENCE_MIGRATION_STEPS =
      List.of(
          STEP_RELATIONSHIP_WIKI_BACKFILL,
          STEP_LEGACY_PARENT_FRONTMATTER,
          STEP_NOTE_SLUG_PATH_REGENERATION);

  /** Max notes processed per HTTP request for each wiki reference migration step. */
  public static final int WIKI_REFERENCE_MIGRATION_BATCH_SIZE = 10;

  private final NoteRepository noteRepository;
  private final WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService;
  private final JdbcTemplate jdbcTemplate;
  private final AdminDataMigrationBatchWorker batchWorker;

  public AdminDataMigrationService(
      NoteRepository noteRepository,
      WikiReferenceMigrationProgressService wikiReferenceMigrationProgressService,
      JdbcTemplate jdbcTemplate,
      @Lazy AdminDataMigrationBatchWorker batchWorker) {
    this.noteRepository = noteRepository;
    this.wikiReferenceMigrationProgressService = wikiReferenceMigrationProgressService;
    this.jdbcTemplate = jdbcTemplate;
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
    if (migrationFullyComplete()) {
      AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
      dto.setMessage("Wiki reference migration is already complete.");
      populateMigrationProgress(dto);
      return dto;
    }
    try {
      return batchWorker.executeBatch(adminUser);
    } catch (RuntimeException e) {
      String step = stepNameForRecordedFailureMark();
      wikiReferenceMigrationProgressService.markFailed(step, errorMessageSafe(e));
      return dtoAfterFailure(step, e.getMessage());
    }
  }

  private static String errorMessageSafe(RuntimeException e) {
    String m = e.getMessage();
    if (m != null && m.length() > 65535) {
      return m.substring(0, 65535);
    }
    return m != null ? m : "";
  }

  /**
   * If the transactional batch aborted, completion flags still reflect state before failure; derive
   * which step failed for {@link WikiReferenceMigrationProgressService#markFailed}.
   */
  private String stepNameForRecordedFailureMark() {
    if (!stepCompleted(STEP_RELATIONSHIP_WIKI_BACKFILL)) {
      return STEP_RELATIONSHIP_WIKI_BACKFILL;
    }
    if (!stepCompleted(STEP_LEGACY_PARENT_FRONTMATTER)) {
      return STEP_LEGACY_PARENT_FRONTMATTER;
    }
    return STEP_NOTE_SLUG_PATH_REGENERATION;
  }

  @Override
  public void populateMigrationProgress(AdminDataMigrationStatusDTO dto) {
    Optional<WikiReferenceMigrationProgress> failed =
        WIKI_REFERENCE_MIGRATION_STEPS.stream()
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
      return totalCountForProgress(
          noteRepository.countRelationshipNotesEligibleForWikiReferenceMigration());
    }
    if (STEP_LEGACY_PARENT_FRONTMATTER.equals(activeStep)) {
      return totalCountForProgress(countLegacyChildNotesEligibleForWikiMigration());
    }
    return totalCountForProgress(noteRepository.countNonDeletedNotes());
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
    return WIKI_REFERENCE_MIGRATION_STEPS.stream()
        .mapToInt(
            s ->
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getProcessedCount)
                    .orElse(0))
        .sum();
  }

  private int aggregateTotalWhenComplete() {
    return WIKI_REFERENCE_MIGRATION_STEPS.stream()
        .mapToInt(
            s ->
                wikiReferenceMigrationProgressService
                    .find(s)
                    .map(WikiReferenceMigrationProgress::getTotalCount)
                    .orElse(0))
        .sum();
  }

  private String activeIncompleteStepName() {
    for (String step : WIKI_REFERENCE_MIGRATION_STEPS) {
      if (!stepCompleted(step)) {
        return step;
      }
    }
    throw new IllegalStateException("No incomplete wiki reference migration step");
  }

  private boolean migrationFullyComplete() {
    return WIKI_REFERENCE_MIGRATION_STEPS.stream().allMatch(this::stepCompleted);
  }

  private boolean stepCompleted(String stepName) {
    return wikiReferenceMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }

  private static int totalCountForProgress(long total) {
    if (total > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) total;
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

  private long countLegacyChildNotesEligibleForWikiMigration() {
    Long c =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note WHERE deleted_at IS NULL AND parent_id IS NOT NULL "
                + "AND target_note_id IS NULL",
            Long.class);
    return c == null ? 0 : c;
  }

  private Optional<String> findFailedStepName() {
    for (String s : WIKI_REFERENCE_MIGRATION_STEPS) {
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
