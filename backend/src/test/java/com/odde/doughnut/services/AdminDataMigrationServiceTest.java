package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.odde.doughnut.controllers.dto.AdminDataMigrationDryRunDTO;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationCollisionGroupDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationNotePreviewDTO;
import com.odde.doughnut.entities.Folder;
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

  @Test
  void dryRun_previewsTitleAndContentChanges_withoutMutatingNotes() {
    Note migratable = makeMe.aNote().title("colour／color").content("## body\n").please();
    Note unchanged = makeMe.aNote().title("plain").content("text").please();
    String migratableTitleBefore = migratable.getTitle();
    String migratableContentBefore = migratable.getContent();
    String unchangedTitleBefore = unchanged.getTitle();
    String unchangedContentBefore = unchanged.getContent();

    AdminDataMigrationDryRunDTO dryRun = adminDataMigrationService.dryRun();

    TitleAliasMigrationNotePreviewDTO migratablePreview =
        dryRun.getNotePreviews().stream()
            .filter(p -> p.getNoteId() == migratable.getId())
            .findFirst()
            .orElseThrow();
    assertThat(migratablePreview.getStatus(), equalTo("MIGRATE"));
    assertThat(migratablePreview.getPlannedTitle(), equalTo("colour"));
    assertThat(migratablePreview.getPlannedAliases(), contains("color"));
    assertThat(migratablePreview.getPlannedContent(), containsString("- color"));

    TitleAliasMigrationNotePreviewDTO unchangedPreview =
        dryRun.getNotePreviews().stream()
            .filter(p -> p.getNoteId() == unchanged.getId())
            .findFirst()
            .orElseThrow();
    assertThat(unchangedPreview.getStatus(), equalTo("NO_CHANGES"));
    assertThat(unchangedPreview.getPlannedTitle(), equalTo("plain"));
    assertThat(unchangedPreview.getPlannedContent(), equalTo("text"));

    assertThat(dryRun.getTotalNoteCount(), equalTo(dryRun.getNotePreviews().size()));
    assertThat(
        dryRun.getMigrateCount() + dryRun.getNoChangesCount(), equalTo(dryRun.getTotalNoteCount()));
    assertThat(dryRun.getMigrateCount(), greaterThanOrEqualTo(1));

    assertThat(migratable.getTitle(), equalTo(migratableTitleBefore));
    assertThat(migratable.getContent(), equalTo(migratableContentBefore));
    assertThat(unchanged.getTitle(), equalTo(unchangedTitleBefore));
    assertThat(unchanged.getContent(), equalTo(unchangedContentBefore));
  }

  @Test
  void dryRun_reportsTitleCollisionsWithPlannedDisambiguation_withoutMutatingNotes() {
    Note keeper = makeMe.aNote().title("colour").please();
    Note firstMigratable =
        makeMe.aNote().underSameNotebookAs(keeper).title("colour／color").please();
    Note secondMigratable = makeMe.aNote().underSameNotebookAs(keeper).title("colour／hue").please();
    String keeperTitleBefore = keeper.getTitle();
    String firstTitleBefore = firstMigratable.getTitle();
    String secondTitleBefore = secondMigratable.getTitle();

    AdminDataMigrationDryRunDTO dryRun = adminDataMigrationService.dryRun();

    assertThat(dryRun.getCollisionGroupCount(), equalTo(1));
    assertThat(dryRun.getCollisionNoteCount(), equalTo(3));
    TitleAliasMigrationCollisionGroupDTO group = dryRun.getCollisionGroups().getFirst();
    assertThat(group.getBasePlannedTitle(), equalTo("colour"));
    assertThat(
        group.getMembers().stream().map(m -> m.getNoteId()).toList(),
        contains(keeper.getId(), firstMigratable.getId(), secondMigratable.getId()));

    TitleAliasMigrationNotePreviewDTO keeperPreview = previewFor(dryRun, keeper.getId());
    TitleAliasMigrationNotePreviewDTO firstPreview = previewFor(dryRun, firstMigratable.getId());
    TitleAliasMigrationNotePreviewDTO secondPreview = previewFor(dryRun, secondMigratable.getId());
    assertThat(keeperPreview.getPlannedTitle(), equalTo("colour"));
    assertThat(firstPreview.getPlannedTitle(), equalTo("colour (1)"));
    assertThat(secondPreview.getPlannedTitle(), equalTo("colour (2)"));

    assertThat(keeper.getTitle(), equalTo(keeperTitleBefore));
    assertThat(firstMigratable.getTitle(), equalTo(firstTitleBefore));
    assertThat(secondMigratable.getTitle(), equalTo(secondTitleBefore));
  }

  @Test
  void dryRun_doesNotReportCollisionsAcrossDifferentFolders() {
    Note anchor = makeMe.aNote().title("anchor").please();
    Folder folderA = makeMe.aFolder().notebook(anchor.getNotebook()).name("A").please();
    Folder folderB = makeMe.aFolder().notebook(anchor.getNotebook()).name("B").please();
    Note inA = makeMe.aNote().title("colour／color").folder(folderA).please();
    Note inB = makeMe.aNote().title("colour／hue").folder(folderB).please();

    AdminDataMigrationDryRunDTO dryRun = adminDataMigrationService.dryRun();

    assertThat(dryRun.getCollisionGroupCount(), equalTo(0));
    assertThat(previewFor(dryRun, inA.getId()).getPlannedTitle(), equalTo("colour"));
    assertThat(previewFor(dryRun, inB.getId()).getPlannedTitle(), equalTo("colour"));
  }

  @Test
  void dryRun_reportsExistingQualifierCollisionDisambiguation() {
    Note keeper = makeMe.aNote().title("cat (animal)").please();
    Note migratable =
        makeMe.aNote().underSameNotebookAs(keeper).title("cat／kitten (animal)").please();

    AdminDataMigrationDryRunDTO dryRun = adminDataMigrationService.dryRun();

    assertThat(dryRun.getCollisionGroupCount(), equalTo(1));
    assertThat(previewFor(dryRun, keeper.getId()).getPlannedTitle(), equalTo("cat (animal)"));
    assertThat(previewFor(dryRun, migratable.getId()).getPlannedTitle(), equalTo("cat (animal 1)"));
    assertThat(keeper.getTitle(), equalTo("cat (animal)"));
    assertThat(migratable.getTitle(), equalTo("cat／kitten (animal)"));
  }

  private static TitleAliasMigrationNotePreviewDTO previewFor(
      AdminDataMigrationDryRunDTO dryRun, int noteId) {
    return dryRun.getNotePreviews().stream()
        .filter(p -> p.getNoteId() == noteId)
        .findFirst()
        .orElseThrow();
  }
}
