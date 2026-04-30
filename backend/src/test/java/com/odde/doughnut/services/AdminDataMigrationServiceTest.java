package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
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

  private void runWikiReferenceMigrationToCompletion() {
    for (int i = 0; i < 100; i++) {
      AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch();
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

    AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch();

    assertThat(dto.getMessage(), containsString("Title backfill"));
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
  void runBatch_detailsBackfillIsIdempotent() {
    Note parent = makeMe.aNote().title("P").please();
    Note target = makeMe.aNote().title("Q").under(parent).please();
    makeMe.aRelation().between(parent, target, RelationType.PART).please();

    runWikiReferenceMigrationToCompletion();

    AdminDataMigrationStatusDTO second = adminDataMigrationService.runBatch();
    assertThat(second.isWikiReferenceMigrationComplete(), is(true));
    assertThat(second.getMessage(), containsString("already complete"));
  }
}
