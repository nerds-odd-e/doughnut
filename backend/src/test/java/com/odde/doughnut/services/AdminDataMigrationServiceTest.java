package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminDataMigrationServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired AdminDataMigrationService adminDataMigrationService;

  @Test
  void getStatus_reportsRegisteredTitleAliasStepAsPending() {
    AdminDataMigrationStatusDTO dto = adminDataMigrationService.getStatus();

    assertThat(dto.getMessage(), equalTo(AdminDataMigrationService.READY_MESSAGE));
    assertThat(dto.isDataMigrationComplete(), equalTo(false));
    assertThat(
        dto.getCurrentStepName(),
        equalTo(AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER));
    assertThat(dto.getStepStatus(), equalTo(WikiReferenceMigrationStepStatus.PENDING.name()));
    assertThat(dto.getProcessedCount(), equalTo(0));
    assertThat(dto.getTotalCount(), equalTo(0));
  }

  @Test
  void runBatch_titleAliasStep_isNoOp_andDoesNotMutateNotes() {
    Note note = makeMe.aNote().title("colour／color").please();
    String titleBefore = note.getTitle();

    AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch(makeMe.anAdmin().please());

    assertThat(dto.getMessage(), containsString("title_alias_to_frontmatter"));
    assertThat(dto.getMessage(), containsString("transform not yet implemented"));
    assertThat(dto.isDataMigrationComplete(), equalTo(false));
    assertThat(
        dto.getCurrentStepName(),
        equalTo(AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER));
    assertThat(dto.getStepStatus(), equalTo(WikiReferenceMigrationStepStatus.RUNNING.name()));
    assertThat(dto.getProcessedCount(), equalTo(0));
    assertThat(dto.getTotalCount(), equalTo(0));
    assertThat(note.getTitle(), equalTo(titleBefore));
  }

  @Test
  void runBatch_matchesStatusProgressAfterNoOpBatch() {
    AdminDataMigrationStatusDTO batch =
        adminDataMigrationService.runBatch(makeMe.anAdmin().please());
    AdminDataMigrationStatusDTO status = adminDataMigrationService.getStatus();

    assertThat(batch.getStepStatus(), equalTo(status.getStepStatus()));
    assertThat(batch.getCurrentStepName(), equalTo(status.getCurrentStepName()));
    assertThat(batch.getProcessedCount(), equalTo(status.getProcessedCount()));
    assertThat(batch.getTotalCount(), equalTo(status.getTotalCount()));
  }
}
