package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.WikiSlugGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminDataMigrationServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired AdminDataMigrationService adminDataMigrationService;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;

  private User migrationAdmin;

  private User admin() {
    if (migrationAdmin == null) {
      migrationAdmin = makeMe.anAdmin().please();
    }
    return migrationAdmin;
  }

  private void runWikiReferenceMigrationToCompletion() {
    for (int i = 0; i < 200; i++) {
      AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch(admin());
      if (dto.isWikiReferenceMigrationComplete()) {
        return;
      }
      if (WikiReferenceMigrationStepStatus.FAILED.name().equals(dto.getStepStatus())) {
        throw new AssertionError("migration failed: " + dto.getLastError());
      }
    }
    throw new AssertionError("migration did not complete in max iterations");
  }

  @Test
  void runBatch_backfillsNullRelationshipTitleThenSlugReflectsTitle() {
    Note parent = makeMe.aNote().title("Alpha").please();
    Note target = makeMe.aNote().title("Beta").under(parent).please();
    Note relation = makeMe.aRelation().between(parent, target, RelationType.PART).please();

    jdbcTemplate.update("UPDATE note SET title = '' WHERE id = ?", relation.getId());
    makeMe.entityPersister.flush();
    makeMe.entityPersister.refresh(relation);

    runWikiReferenceMigrationToCompletion();

    Note updated = makeMe.entityPersister.find(Note.class, relation.getId());
    String expectedTitle =
        RelationshipNoteTitleFormatter.format("Alpha", RelationType.PART.label, "Beta");
    assertThat(updated.getTitle(), equalTo(expectedTitle));
    assertThat(
        WikiSlugPathAssignment.basenameOf(updated.getSlug()),
        equalTo(WikiSlugGeneration.toBaseSlug(expectedTitle)));
  }

  @Test
  void runBatch_returnsCountInMessage() {
    Note parent = makeMe.aNote().title("A").please();
    Note target = makeMe.aNote().title("B").under(parent).please();
    Note relation = makeMe.aRelation().between(parent, target, RelationType.RELATED_TO).please();
    jdbcTemplate.update("UPDATE note SET title = '' WHERE id = ?", relation.getId());
    makeMe.entityPersister.flush();

    AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch(admin());

    assertThat(dto.getMessage(), containsString("Relationship wiki backfill"));
    assertThat(dto.getMessage(), containsString("1 note"));
    assertThat(dto.isWikiReferenceMigrationComplete(), is(false));
  }

  @Test
  void runBatch_backfillsLegacyRelationshipDetailsPreservingExistingBody() {
    Note parent = makeMe.aNote().title("Source").please();
    Note target = makeMe.aNote().title("Target").under(parent).please();
    Note relation = makeMe.aRelation().between(parent, target, RelationType.APPLICATION).please();
    jdbcTemplate.update(
        "UPDATE note SET details = ? WHERE id = ?",
        "Legacy line one.\n\nLine two.",
        relation.getId());
    makeMe.entityPersister.flush();
    makeMe.entityPersister.refresh(relation);

    runWikiReferenceMigrationToCompletion();

    Note updated = makeMe.entityPersister.find(Note.class, relation.getId());
    assertThat(
        updated.getDetails(),
        equalTo(
            RelationshipNoteMarkdownFormatter.format(
                RelationType.APPLICATION, "Source", "Target", "Legacy line one.\n\nLine two.")));
  }

  @Test
  void runBatch_populatesWikiTitleCacheForRelationshipNotes() {
    User owner = makeMe.aUser().please();
    Note root = makeMe.aNote().title("Root").creatorAndOwner(owner).please();
    Note parent = makeMe.aNote().title("Alpha").under(root).please();
    Note target = makeMe.aNote().title("Beta").under(root).please();
    Note relation = makeMe.aRelation().between(parent, target, RelationType.RELATED_TO).please();
    jdbcTemplate.update("UPDATE note SET title = '' WHERE id = ?", relation.getId());
    makeMe.entityPersister.flush();

    runWikiReferenceMigrationToCompletion();

    assertThat(
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(relation.getId()), not(empty()));
    assertThat(
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(relation.getId()).size(),
        equalTo(2));
    assertThat(
        noteWikiTitleCacheRepository
            .findByNote_IdOrderByIdAsc(relation.getId())
            .get(0)
            .getLinkText(),
        equalTo("Alpha"));
    assertThat(
        noteWikiTitleCacheRepository
            .findByNote_IdOrderByIdAsc(relation.getId())
            .get(1)
            .getLinkText(),
        equalTo("Beta"));
  }

  @Test
  void runBatch_secondFullRunReportsAlreadyComplete() {
    Note parent = makeMe.aNote().title("P").please();
    Note target = makeMe.aNote().title("Q").under(parent).please();
    makeMe.aRelation().between(parent, target, RelationType.PART).please();

    runWikiReferenceMigrationToCompletion();

    AdminDataMigrationStatusDTO second = adminDataMigrationService.runBatch(admin());
    assertThat(second.isWikiReferenceMigrationComplete(), is(true));
    assertThat(second.getMessage(), containsString("already complete"));
  }

  @Test
  void runBatch_slugRegenerationRunsInMultipleHttpSizedBatches() {
    User owner = makeMe.aUser().please();
    Note root = makeMe.aNote().title("Root").creatorAndOwner(owner).please();
    int extra = AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE;
    for (int i = 0; i < extra; i++) {
      makeMe.aNote().title("Leaf" + i).under(root).please();
    }
    makeMe.entityPersister.flush();

    AdminDataMigrationStatusDTO first = adminDataMigrationService.runBatch(admin());
    assertThat(first.getMessage(), containsString("Relationship wiki backfill"));
    assertThat(first.getMessage(), containsString("nothing pending"));

    AdminDataMigrationStatusDTO slugFirst = adminDataMigrationService.runBatch(admin());
    assertThat(
        slugFirst.getCurrentStepName(),
        equalTo(AdminDataMigrationService.STEP_NOTE_SLUG_PATH_REGENERATION));
    assertThat(slugFirst.getMessage(), containsString("Slug regeneration"));
    assertThat(slugFirst.isWikiReferenceMigrationComplete(), is(false));
    assertThat(
        slugFirst.getProcessedCount(),
        equalTo(AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE));

    AdminDataMigrationStatusDTO slugSecond = adminDataMigrationService.runBatch(admin());
    assertThat(slugSecond.isWikiReferenceMigrationComplete(), is(true));
    assertThat(slugSecond.getMessage(), containsString("Slug regeneration"));
  }
}
