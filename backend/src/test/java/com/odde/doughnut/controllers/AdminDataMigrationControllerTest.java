package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AdminDataMigrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AdminDataMigrationControllerTest extends ControllerTestBase {

  @Autowired AdminDataMigrationController controller;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void adminGetsMigrationStatus() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    AdminDataMigrationStatusDTO status = controller.getAdminDataMigrationStatus();

    assertThat(status.getMessage(), equalTo(AdminDataMigrationService.READY_MESSAGE));
  }

  @Test
  void adminRunBatchReturnsBatchSummary() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    AdminDataMigrationStatusDTO run = controller.runDataMigrationBatch();

    assertThat(run.getMessage(), notNullValue());
    assertThat(run.getMessage(), containsString("backfill"));
    assertThat(
        controller.getAdminDataMigrationStatus().getMessage(),
        equalTo(AdminDataMigrationService.READY_MESSAGE));
  }

  @Test
  void adminRunBatchProcessesAtMostBatchSizeTitlesPerRequest()
      throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    Note parent = makeMe.aNote().title("Parent").please();
    int n = AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE + 1;
    for (int i = 0; i < n; i++) {
      Note target = makeMe.aNote().title("Tgt" + i).under(parent).please();
      Note relation = makeMe.aRelation().between(parent, target, RelationType.RELATED_TO).please();
      jdbcTemplate.update("UPDATE note SET title = '' WHERE id = ?", relation.getId());
    }
    makeMe.entityPersister.flush();

    AdminDataMigrationStatusDTO first = controller.runDataMigrationBatch();

    assertThat(
        first.getCurrentStepName(),
        equalTo(AdminDataMigrationService.STEP_RELATIONSHIP_TITLE_BACKFILL));
    assertThat(first.isWikiReferenceMigrationComplete(), is(false));
    assertThat(
        first.getProcessedCount(),
        equalTo(AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE));

    AdminDataMigrationStatusDTO second = controller.runDataMigrationBatch();
    assertThat(second.getMessage(), containsString("Title backfill"));
    assertThat(second.getMessage(), containsString("1 note"));
  }

  @Test
  void nonAdminCannotGetStatus() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getAdminDataMigrationStatus());
  }

  @Test
  void nonAdminCannotRunMigrationBatch() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.runDataMigrationBatch());
  }
}
