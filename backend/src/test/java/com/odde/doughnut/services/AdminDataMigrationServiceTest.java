package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.testability.MakeMe;
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
  void runBatch_backfillsNullRelationshipTitle() {
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
            RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
                updated,
                RelationType.APPLICATION,
                makeMe.entityPersister.find(Note.class, parent.getId()),
                makeMe.entityPersister.find(Note.class, target.getId()),
                "Legacy line one.\n\nLine two.")));
  }

  @Test
  void wikiLinkResolver_findsParentLinkInsideYamlFrontmatter() {
    User owner = makeMe.aUser().please();
    Note parent = makeMe.aNote().title("Alpha").creatorAndOwner(owner).please();
    Note child = makeMe.aNote().title("Child").under(parent).please();
    child.setDetails("---\nparent: \"[[Alpha]]\"\n---\n\nBody line.");
    makeMe.entityPersister.merge(child);
    makeMe.entityPersister.flush();

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Test
  void wikiLinkResolver_findsPlainWikiLinkInBody() {
    User owner = makeMe.aUser().please();
    Note parent = makeMe.aNote().title("Alpha").creatorAndOwner(owner).please();
    Note child = makeMe.aNote().title("Child").under(parent).details("See [[Alpha]]").please();
    makeMe.entityPersister.flush();
    makeMe.entityPersister.refresh(parent);

    assertThat(parent.getChildren().size(), equalTo(1));
    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Autowired WikiLinkResolver wikiLinkResolver;

  @Test
  void runBatch_addsParentFrontmatterAndWikiTitleCacheForOrdinaryChild() {
    Note parent = makeMe.aNote().title("Alpha").please();
    Note child = makeMe.aNote().title("Child").under(parent).details("Body line.").please();

    runWikiReferenceMigrationToCompletion();

    Note updated = makeMe.entityPersister.find(Note.class, child.getId());
    assertThat(updated.getDetails(), containsString("parent: \"[[Alpha]]\""));
    assertThat(updated.getDetails(), containsString("Body line."));
    var rows = noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(child.getId());
    assertThat(rows, not(empty()));
    assertThat(rows.stream().map(r -> r.getLinkText()).toList(), hasItem(equalTo("Alpha")));
    assertThat(
        rows.stream()
            .filter(r -> r.getLinkText().equals("Alpha"))
            .findFirst()
            .orElseThrow()
            .getTargetNote()
            .getId(),
        equalTo(parent.getId()));
  }

  @Test
  void runBatch_mergesParentIntoExistingFrontmatterWithoutRemovingOtherKeys() {
    Note parent = makeMe.aNote().title("P").please();
    Note child = makeMe.aNote().under(parent).details("---\nfoo: bar\n---\n\nBody").please();

    runWikiReferenceMigrationToCompletion();

    Note updated = makeMe.entityPersister.find(Note.class, child.getId());
    assertThat(updated.getDetails(), containsString("foo: bar"));
    assertThat(updated.getDetails(), containsString("parent: \"[[P]]\""));
    assertThat(updated.getDetails(), containsString("Body"));
  }

  @Test
  void runBatch_legacyParentFrontmatterRunsInBatchedChunks() {
    User owner = makeMe.aUser().please();
    Note root = makeMe.aNote().title("Root").creatorAndOwner(owner).please();
    int extra = AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE;
    for (int i = 0; i < extra + 1; i++) {
      makeMe.aNote().title("Leaf" + i).withNoDescription().under(root).please();
    }
    makeMe.entityPersister.flush();

    AdminDataMigrationStatusDTO rel = adminDataMigrationService.runBatch(admin());
    assertThat(rel.getMessage(), containsString("Relationship wiki backfill"));
    assertThat(rel.getMessage(), containsString("nothing pending"));

    AdminDataMigrationStatusDTO leg1 = adminDataMigrationService.runBatch(admin());
    assertThat(leg1.getMessage(), containsString("Legacy parent frontmatter"));
    assertThat(
        leg1.getProcessedCount(),
        equalTo(AdminDataMigrationService.WIKI_REFERENCE_MIGRATION_BATCH_SIZE));

    AdminDataMigrationStatusDTO leg2 = adminDataMigrationService.runBatch(admin());
    assertThat(leg2.getMessage(), containsString("Legacy parent frontmatter"));
    assertThat(leg2.getMessage(), containsString("1 note"));

    AdminDataMigrationStatusDTO wikiRef = adminDataMigrationService.runBatch(admin());
    assertThat(wikiRef.isWikiReferenceMigrationComplete(), is(true));
    assertThat(wikiRef.getMessage(), containsString("Relationship wiki reference refresh"));
  }

  @Test
  void newChildNoteDetailsDoNotIncludeParentFrontmatterBeforeMigration() {
    Note parent = makeMe.aNote().title("Root").please();
    Note child = makeMe.aNote().title("Fresh").withNoDescription().under(parent).please();

    assertThat(child.getDetails(), not(containsString("parent:")));
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
  void obsolete_failed_progress_row_for_retired_step_does_not_block_migration_completion() {
    jdbcTemplate.update(
        "INSERT INTO wiki_reference_migration_progress (step_name, status, total_count,"
            + " processed_count, last_error) VALUES (?, ?, ?, ?, ?)",
        "retired_non_gating_step",
        WikiReferenceMigrationStepStatus.FAILED.name(),
        27377,
        2220,
        "obsolete batch failure");

    Note parent = makeMe.aNote().title("P").please();
    Note target = makeMe.aNote().title("Q").under(parent).please();
    makeMe.aRelation().between(parent, target, RelationType.PART).please();

    runWikiReferenceMigrationToCompletion();

    assertThat(adminDataMigrationService.getStatus().isWikiReferenceMigrationComplete(), is(true));
  }

  @Test
  void runBatch_relationshipWikiReferenceRefresh_qualifiesCrossNotebookTargetsAndRefreshesCache() {
    User owner = admin();
    Note nbARoot = makeMe.aNote().title("NotebookA").creatorAndOwner(owner).please();
    Note nbBRoot = makeMe.aNote().title("NotebookB").creatorAndOwner(owner).please();
    Note source = makeMe.aNote().title("SourceTitle").under(nbARoot).please();
    Note target = makeMe.aNote().title("TargetTitle").under(nbBRoot).please();
    Note relation = makeMe.aRelation().between(source, target, RelationType.RELATED_TO).please();

    runWikiReferenceMigrationToCompletion();

    Note updated = makeMe.entityPersister.find(Note.class, relation.getId());
    assertThat(updated.getDetails(), containsString("source: \"[[SourceTitle]]\""));
    assertThat(updated.getDetails(), containsString("target: \"[[NotebookB: TargetTitle]]\""));
    assertThat(
        updated.getDetails(),
        containsString("[[SourceTitle]] related to [[NotebookB: TargetTitle]]."));

    var rows = noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(updated.getId());
    assertThat(
        rows.stream().map(NoteWikiTitleCache::getLinkText).toList(),
        hasItems("SourceTitle", "NotebookB: TargetTitle"));
    Note loadedSource = makeMe.entityPersister.find(Note.class, source.getId());
    Note loadedTarget = makeMe.entityPersister.find(Note.class, target.getId());
    assertThat(
        rows.stream()
            .filter(r -> r.getLinkText().equals("SourceTitle"))
            .findFirst()
            .orElseThrow()
            .getTargetNote()
            .getId(),
        equalTo(loadedSource.getId()));
    assertThat(
        rows.stream()
            .filter(r -> r.getLinkText().equals("NotebookB: TargetTitle"))
            .findFirst()
            .orElseThrow()
            .getTargetNote()
            .getId(),
        equalTo(loadedTarget.getId()));
  }

  @Test
  void runBatch_relationshipWikiReferenceRefresh_keepsUnqualifiedLinksForSameNotebook() {
    User owner = admin();
    Note root = makeMe.aNote().title("Home").creatorAndOwner(owner).please();
    Note source = makeMe.aNote().title("Alpha").under(root).please();
    Note target = makeMe.aNote().title("Beta").under(root).please();
    Note relation = makeMe.aRelation().between(source, target, RelationType.RELATED_TO).please();

    runWikiReferenceMigrationToCompletion();

    Note updated = makeMe.entityPersister.find(Note.class, relation.getId());
    assertThat(updated.getDetails(), containsString("source: \"[[Alpha]]\""));
    assertThat(updated.getDetails(), containsString("target: \"[[Beta]]\""));
    assertThat(updated.getDetails(), containsString("[[Alpha]] related to [[Beta]]."));
    assertThat(
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(updated.getId()).stream()
            .map(NoteWikiTitleCache::getLinkText)
            .toList(),
        hasItems("Alpha", "Beta"));
  }

  @Test
  void runBatch_relationshipWikiReferenceRefresh_preservesUserSuffixAfterRewrite() {
    User owner = admin();
    Note root = makeMe.aNote().title("Home").creatorAndOwner(owner).please();
    Note source = makeMe.aNote().title("Moon").under(root).please();
    Note target = makeMe.aNote().title("Earth").under(root).please();
    Note relation = makeMe.aRelation().between(source, target, RelationType.PART).please();

    String seeded =
        RelationshipNoteMarkdownFormatter.format(RelationType.PART, "Moon", "Earth", null)
            + "\n\nUser scribble below.";
    jdbcTemplate.update("UPDATE note SET details = ? WHERE id = ?", seeded, relation.getId());
    makeMe.entityPersister.flush();
    makeMe.entityPersister.refresh(relation);

    runWikiReferenceMigrationToCompletion();

    Note updated = makeMe.entityPersister.find(Note.class, relation.getId());
    assertThat(updated.getDetails(), containsString("User scribble below."));
    assertThat(
        updated.getDetails(),
        equalTo(
            RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
                updated,
                RelationType.PART,
                makeMe.entityPersister.find(Note.class, source.getId()),
                makeMe.entityPersister.find(Note.class, target.getId()),
                "User scribble below.")));
  }
}
