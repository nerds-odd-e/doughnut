package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.AdminDataMigrationDryRunDTO;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.controllers.dto.TitleAliasInboundReferenceRewritePreviewDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AdminDataMigrationService;
import com.odde.doughnut.services.WikiTitleCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminDataMigrationControllerTest extends ControllerTestBase {

  @Autowired AdminDataMigrationController controller;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  @Test
  void adminGetsMigrationStatus() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    AdminDataMigrationStatusDTO status = controller.getAdminDataMigrationStatus();

    assertThat(status.getMessage(), equalTo(AdminDataMigrationService.READY_MESSAGE));
    assertThat(status.isDataMigrationComplete(), equalTo(false));
    assertThat(
        status.getCurrentStepName(),
        equalTo(AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER));
    assertThat(status.getStepStatus(), equalTo(WikiReferenceMigrationStepStatus.PENDING.name()));
  }

  @Test
  void adminRunBatchReturnsBatchSummary() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    makeMe.aNote().title("colour／color").please();

    AdminDataMigrationStatusDTO run = controller.runDataMigrationBatch();

    assertThat(run.getMessage(), notNullValue());
    assertThat(run.getMessage(), containsString("title_alias_to_frontmatter"));

    AdminDataMigrationStatusDTO inboundRewrite = controller.runDataMigrationBatch();
    assertThat(
        inboundRewrite.getMessage(), containsString("title_alias_inbound_reference_rewrite"));
    assertThat(controller.getAdminDataMigrationStatus().isDataMigrationComplete(), equalTo(true));
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

  @Test
  void adminGetsDryRunPreviewWithoutMutatingNotes() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    var note = makeMe.aNote().title("colour／color").content("body").please();
    String titleBefore = note.getTitle();
    String contentBefore = note.getContent();

    AdminDataMigrationDryRunDTO dryRun = controller.getAdminDataMigrationDryRun();

    assertThat(
        dryRun.getNotePreviews().stream()
            .filter(p -> p.getNoteId() == note.getId())
            .findFirst()
            .orElseThrow()
            .getStatus(),
        equalTo("MIGRATE"));
    assertThat(note.getTitle(), equalTo(titleBefore));
    assertThat(note.getContent(), equalTo(contentBefore));
  }

  @Test
  void adminGetsDryRunCollisionReportWithoutMutatingNotes()
      throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    var keeper = makeMe.aNote().title("colour").please();
    var migratable = makeMe.aNote().underSameNotebookAs(keeper).title("colour／color").please();
    String keeperTitleBefore = keeper.getTitle();
    String migratableTitleBefore = migratable.getTitle();

    AdminDataMigrationDryRunDTO dryRun = controller.getAdminDataMigrationDryRun();

    assertThat(dryRun.getCollisionGroupCount(), equalTo(1));
    assertThat(dryRun.getCollisionGroups(), hasSize(1));
    assertThat(
        dryRun.getNotePreviews().stream()
            .filter(p -> p.getNoteId() == migratable.getId())
            .findFirst()
            .orElseThrow()
            .getPlannedTitle(),
        equalTo("colour (1)"));
    assertThat(keeper.getTitle(), equalTo(keeperTitleBefore));
    assertThat(migratable.getTitle(), equalTo(migratableTitleBefore));
  }

  @Test
  void adminGetsInboundReferenceRewriteDryRunWithoutMutatingNotes()
      throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    var owner = makeMe.aUser().please();
    Note target = makeMe.aNote().notebookOwnedBy(owner).title("colour／color").please();
    Note referrer = makeMe.aNote().underSameNotebookAs(target).content("[[colour／color]]").please();
    wikiTitleCacheService.refreshForNote(referrer, owner);
    String referrerContentBefore = referrer.getContent();

    AdminDataMigrationStatusDTO run = controller.runDataMigrationBatch();
    while (AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER.equals(
        controller.getAdminDataMigrationStatus().getCurrentStepName())) {
      run = controller.runDataMigrationBatch();
    }
    assertThat(run.isDataMigrationComplete(), equalTo(false));

    AdminDataMigrationDryRunDTO dryRun = controller.getAdminDataMigrationDryRun();

    TitleAliasInboundReferenceRewritePreviewDTO preview =
        dryRun.getInboundReferenceRewritePreviews().getFirst();
    assertThat(preview.getPlannedLinkInner(), equalTo("colour"));
    assertThat(preview.isVisibleTextWillChange(), equalTo(true));
    assertThat(referrer.getContent(), equalTo(referrerContentBefore));
  }

  @Test
  void nonAdminCannotGetDryRun() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getAdminDataMigrationDryRun());
  }

  @Test
  void adminGetsAccurateStatusAfterPartialBatch() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    Note anchor = makeMe.aNote().title("note0／alias0").please();
    for (int i = 1; i <= AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE; i++) {
      makeMe.aNote().underSameNotebookAs(anchor).title("note" + i + "／alias" + i).please();
    }

    controller.runDataMigrationBatch();
    AdminDataMigrationStatusDTO status = controller.getAdminDataMigrationStatus();

    assertThat(status.isDataMigrationComplete(), equalTo(false));
    assertThat(
        status.getProcessedCount(), equalTo(AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE));
    assertThat(
        status.getTotalCount(), equalTo(AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE + 1));
  }

  @Test
  void adminRepeatedRunBatchAfterCompletion_isIdempotent() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    var note = makeMe.aNote().title("colour／color").please();
    String titleAfter = "colour";

    controller.runDataMigrationBatch();
    while (!controller.getAdminDataMigrationStatus().isDataMigrationComplete()) {
      controller.runDataMigrationBatch();
    }
    assertThat(note.getTitle(), equalTo(titleAfter));

    AdminDataMigrationStatusDTO secondRun = controller.runDataMigrationBatch();

    assertThat(secondRun.getMessage(), containsString("already complete"));
    assertThat(note.getTitle(), equalTo(titleAfter));
    assertThat(controller.getAdminDataMigrationStatus().isDataMigrationComplete(), equalTo(true));
  }
}
