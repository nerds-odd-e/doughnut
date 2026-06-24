package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import com.odde.doughnut.controllers.dto.AdminDataMigrationDryRunDTO;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationCollisionGroupDTO;
import com.odde.doughnut.controllers.dto.TitleAliasMigrationNotePreviewDTO;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAliasIndex;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteAliasIndexRepository;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
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
  @Autowired NoteAliasIndexRepository noteAliasIndexRepository;

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
  void runBatch_migratesNonCollidingNotes_andRefreshesAliasIndex() {
    Note note = makeMe.aNote().title("colour／color").content("## body\n").please();

    AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch(makeMe.anAdmin().please());

    assertThat(dto.isDataMigrationComplete(), equalTo(true));
    assertThat(dto.getStepStatus(), equalTo(WikiReferenceMigrationStepStatus.COMPLETED.name()));
    assertThat(dto.getMessage(), containsString("complete"));
    assertThat(note.getTitle(), equalTo("colour"));
    assertThat(note.getContent(), containsString("- color"));
    List<NoteAliasIndex> aliasRows =
        noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
    assertThat(aliasRows, hasSize(1));
    assertThat(aliasRows.getFirst().getAliasDisplay(), equalTo("color"));
  }

  @Test
  void runBatch_mergesTitleAliasesWithExistingFrontmatterAliases() {
    String content = "---\naliases:\n  - hue\n---\n\nbody";
    Note note = makeMe.aNote().title("colour／color").content(content).please();

    adminDataMigrationService.runBatch(makeMe.anAdmin().please());

    assertThat(note.getTitle(), equalTo("colour"));
    assertThat(note.getContent(), containsString("- hue"));
    assertThat(note.getContent(), containsString("- color"));
    assertThat(
        note.getContent().indexOf("- hue"), is(lessThan(note.getContent().indexOf("- color"))));
    List<NoteAliasIndex> aliasRows =
        noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
    assertThat(aliasRows, hasSize(2));
    assertThat(aliasRows.get(0).getAliasDisplay(), equalTo("hue"));
    assertThat(aliasRows.get(1).getAliasDisplay(), equalTo("color"));
  }

  @Test
  void runBatch_skipsCollidingNotes_andReportsPendingCollisionHandling() {
    Note keeper = makeMe.aNote().title("colour").please();
    Note migratable = makeMe.aNote().underSameNotebookAs(keeper).title("colour／color").please();
    String keeperTitleBefore = keeper.getTitle();
    String migratableTitleBefore = migratable.getTitle();

    AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch(makeMe.anAdmin().please());

    assertThat(dto.getMessage(), containsString("pending disambiguation"));
    assertThat(dto.isDataMigrationComplete(), equalTo(false));
    assertThat(dto.getStepStatus(), equalTo(WikiReferenceMigrationStepStatus.RUNNING.name()));
    assertThat(keeper.getTitle(), equalTo(keeperTitleBefore));
    assertThat(migratable.getTitle(), equalTo(migratableTitleBefore));
    assertThat(noteAliasIndexRepository.findByNote_IdOrderByIdAsc(keeper.getId()), hasSize(0));
    assertThat(noteAliasIndexRepository.findByNote_IdOrderByIdAsc(migratable.getId()), hasSize(0));
  }

  @Test
  void runBatch_withOnlyUnmigratableNotes_completesWithoutMutatingNotes() {
    Note note = makeMe.aNote().title("plain").content("body").please();
    String titleBefore = note.getTitle();
    String contentBefore = note.getContent();

    AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch(makeMe.anAdmin().please());

    assertThat(dto.isDataMigrationComplete(), equalTo(true));
    assertThat(dto.getStepStatus(), equalTo(WikiReferenceMigrationStepStatus.COMPLETED.name()));
    assertThat(note.getTitle(), equalTo(titleBefore));
    assertThat(note.getContent(), equalTo(contentBefore));
  }

  @Test
  void runBatch_respectsBatchSizeAndResumesOnNextCall() {
    var admin = makeMe.anAdmin().please();
    Note anchor = makeMe.aNote().title("note0／alias0").please();
    for (int i = 1; i <= AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE; i++) {
      makeMe.aNote().underSameNotebookAs(anchor).title("note" + i + "／alias" + i).please();
    }

    AdminDataMigrationStatusDTO first = adminDataMigrationService.runBatch(admin);
    AdminDataMigrationStatusDTO second = adminDataMigrationService.runBatch(admin);

    assertThat(
        first.getProcessedCount(), equalTo(AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE));
    assertThat(first.isDataMigrationComplete(), equalTo(false));
    assertThat(
        second.getProcessedCount(),
        greaterThan(AdminDataMigrationService.DATA_MIGRATION_BATCH_SIZE));
    assertThat(second.isDataMigrationComplete(), equalTo(true));
  }

  @Test
  void runBatch_matchesStatusProgressAfterBatch() {
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
