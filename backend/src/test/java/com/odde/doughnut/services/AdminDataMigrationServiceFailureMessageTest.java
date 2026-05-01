package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

class AdminDataMigrationServiceFailureMessageTest {

  @Test
  void runBatch_failureMessageIncludesDeploymentMarkerAndMigrationContext() {
    NoteRepository noteRepository = mock(NoteRepository.class);
    WikiReferenceMigrationProgressService progressService =
        mock(WikiReferenceMigrationProgressService.class);
    AdminDataMigrationBatchWorker batchWorker = mock(AdminDataMigrationBatchWorker.class);
    AdminDataMigrationService service =
        new AdminDataMigrationService(
            noteRepository, progressService, mock(JdbcTemplate.class), batchWorker);
    when(progressService.find(AdminDataMigrationService.STEP_RELATIONSHIP_WIKI_BACKFILL))
        .thenReturn(
            OptionalProgress.completed(AdminDataMigrationService.STEP_RELATIONSHIP_WIKI_BACKFILL));
    when(progressService.find(AdminDataMigrationService.STEP_LEGACY_PARENT_FRONTMATTER))
        .thenReturn(
            OptionalProgress.completed(AdminDataMigrationService.STEP_LEGACY_PARENT_FRONTMATTER));
    when(progressService.find(AdminDataMigrationService.STEP_NOTE_SLUG_PATH_REGENERATION))
        .thenReturn(
            OptionalProgress.running(AdminDataMigrationService.STEP_NOTE_SLUG_PATH_REGENERATION));
    when(batchWorker.executeBatch(Mockito.any())).thenThrow(new RuntimeException("duplicate slug"));

    AdminDataMigrationStatusDTO dto = service.runBatch(new User());

    assertThat(dto.getMessage(), containsString(AdminDataMigrationService.DIAGNOSTIC_MARKER));
    assertThat(dto.getMessage(), containsString("step=note_slug_path_regeneration"));
    assertThat(dto.getMessage(), containsString("batchSize=10"));
    assertThat(dto.getMessage(), containsString("noteSlugMaxLen=767"));
    assertThat(dto.getMessage(), containsString("duplicate slug"));
    verify(progressService)
        .markFailed(
            Mockito.eq(AdminDataMigrationService.STEP_NOTE_SLUG_PATH_REGENERATION),
            Mockito.contains(AdminDataMigrationService.DIAGNOSTIC_MARKER));
  }

  private static class OptionalProgress {
    static java.util.Optional<WikiReferenceMigrationProgress> completed(String step) {
      return java.util.Optional.of(progress(step, WikiReferenceMigrationStepStatus.COMPLETED));
    }

    static java.util.Optional<WikiReferenceMigrationProgress> running(String step) {
      return java.util.Optional.of(progress(step, WikiReferenceMigrationStepStatus.RUNNING));
    }

    private static WikiReferenceMigrationProgress progress(
        String step, WikiReferenceMigrationStepStatus status) {
      WikiReferenceMigrationProgress p = new WikiReferenceMigrationProgress();
      p.setStepName(step);
      p.setStatus(status);
      p.setProcessedCount(2220);
      p.setTotalCount(27377);
      return p;
    }
  }
}
