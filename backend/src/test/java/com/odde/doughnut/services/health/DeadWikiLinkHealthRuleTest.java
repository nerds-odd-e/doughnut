package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.NoteAliasIndexService;
import com.odde.doughnut.services.NotebookHealthService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeadWikiLinkHealthRuleTest {
  @Autowired NotebookHealthService notebookHealthService;
  @Autowired NoteAliasIndexService noteAliasIndexService;
  @Autowired NoteRepository noteRepository;
  @Autowired MakeMe makeMe;

  private User owner;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    owner = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
  }

  @Test
  void reportsBodyDeadWikiLinkNestedUnderSourceNote() {
    Note source =
        makeMe.aNote().title("Source").notebook(notebook).content("See [[Missing]]").please();

    HealthFindingGroup group = deadWikiLinksGroup();
    HealthFindingGroup child = soleChild(group);

    assertThat(child.getTitle(), equalTo("Source"));
    assertThat(child.getRuleId(), equalTo(HealthRuleIds.DEAD_WIKI_LINKS));
    assertThat(child.isAutoFixable(), equalTo(false));
    assertThat(child.getItems(), hasSize(1));
    HealthFindingItem item = child.getItems().getFirst();
    assertThat(item.getNoteId(), equalTo(source.getId()));
    assertThat(item.getWikiLinkToken(), equalTo("Missing"));
    assertThat(item.getLabel(), equalTo("Missing"));
    assertThat(item.getFolderId(), is(nullValue()));
  }

  @Test
  void reportsFrontmatterDeadWikiLinkWithSameTokenShape() {
    Note source = makeMe.aNote().title("WithFm").notebook(notebook).please();
    source.setContent("---\nparent: \"[[Missing]]\"\n---\n\nBody line.");
    makeMe.entityPersister.merge(source);
    makeMe.entityPersister.flush();

    HealthFindingGroup child = soleChild(deadWikiLinksGroup());
    HealthFindingItem item = child.getItems().getFirst();
    assertThat(item.getNoteId(), equalTo(source.getId()));
    assertThat(item.getWikiLinkToken(), equalTo("Missing"));
    assertThat(item.getLabel(), equalTo("Missing"));
  }

  @Test
  void doesNotReportLiveBodyLinkToExistingTitle() {
    makeMe.aNote().title("Alpha").notebook(notebook).please();
    makeMe.aNote().title("Child").notebook(notebook).content("See [[Alpha]]").please();

    assertThat(deadWikiLinksGroup().getChildren(), empty());
  }

  @Test
  void doesNotReportLiveAliasAfterRefresh() {
    String aliasTargetMarkdown = "---\naliases:\n  - color\n---\n\nbody";
    Note aliasTarget =
        makeMe.aNote().title("colour").notebook(notebook).content(aliasTargetMarkdown).please();
    noteAliasIndexService.refreshForNote(aliasTarget);
    makeMe.aNote().notebook(notebook).content("See [[color]]").please();

    assertThat(deadWikiLinksGroup().getChildren(), empty());
  }

  @Test
  void doesNotReportLiveQualifiedNotebookTitleForOwner() {
    Notebook otherNotebook =
        makeMe.aNotebook().creatorAndOwner(owner).name("Other Notebook").please();
    makeMe.aNote().title("Target").notebook(otherNotebook).please();
    makeMe
        .aNote()
        .title("Linker")
        .notebook(notebook)
        .content("See [[Other Notebook:Target]]")
        .please();

    assertThat(deadWikiLinksGroup().getChildren(), empty());
  }

  @Test
  void excludesSoftDeletedSourceNotesFromAudit() {
    makeMe
        .aNote()
        .title("Gone")
        .notebook(notebook)
        .content("See [[Missing]]")
        .softDeleted()
        .please();
    Note live =
        makeMe.aNote().title("Live").notebook(notebook).content("See [[AlsoMissing]]").please();

    HealthFindingGroup group = deadWikiLinksGroup();
    assertThat(group.getChildren(), hasSize(1));
    assertThat(group.getChildren().getFirst().getTitle(), equalTo("Live"));
    assertThat(
        group.getChildren().getFirst().getItems().getFirst().getNoteId(), equalTo(live.getId()));
  }

  @Test
  void reportsMissingAndSoftDeletedTargetsAsDead() {
    Note softTarget = makeMe.aNote().title("SoftTarget").notebook(notebook).softDeleted().please();
    Note linker =
        makeMe
            .aNote()
            .title("Linker")
            .notebook(notebook)
            .content("See [[SoftTarget]] and [[NeverExisted]]")
            .please();
    makeMe.entityPersister.refresh(softTarget);

    HealthFindingGroup child = soleChild(deadWikiLinksGroup());
    assertThat(child.getItems().getFirst().getNoteId(), equalTo(linker.getId()));
    assertThat(
        child.getItems().stream().map(HealthFindingItem::getWikiLinkToken).toList(),
        equalTo(List.of("SoftTarget", "NeverExisted")));
  }

  @Test
  void reportsEachDistinctUnresolvedTokenOnceInFirstOccurrenceOrder() {
    makeMe
        .aNote()
        .title("Dedupe")
        .notebook(notebook)
        .content("[[Missing]] then [[Missing]] again")
        .please();

    HealthFindingGroup child = soleChild(deadWikiLinksGroup());
    assertThat(child.getItems(), hasSize(1));
    assertThat(child.getItems().getFirst().getWikiLinkToken(), equalTo("Missing"));
  }

  @Test
  void nestsDeadLinksByNoteWithEmptyTopItems() {
    Note first = makeMe.aNote().title("First").notebook(notebook).content("[[DeadA]]").please();
    Note second = makeMe.aNote().title("Second").notebook(notebook).content("[[DeadB]]").please();

    HealthFindingGroup group = deadWikiLinksGroup();
    assertThat(group.getItems() == null || group.getItems().isEmpty(), is(true));
    assertThat(group.getChildren(), hasSize(2));
    assertThat(group.getChildren().get(0).getTitle(), equalTo("First"));
    assertThat(
        group.getChildren().get(0).getItems().getFirst().getNoteId(), equalTo(first.getId()));
    assertThat(
        group.getChildren().get(0).getItems().getFirst().getWikiLinkToken(), equalTo("DeadA"));
    assertThat(group.getChildren().get(1).getTitle(), equalTo("Second"));
    assertThat(
        group.getChildren().get(1).getItems().getFirst().getNoteId(), equalTo(second.getId()));
    assertThat(
        group.getChildren().get(1).getItems().getFirst().getWikiLinkToken(), equalTo("DeadB"));
    assertThat(group.getChildren().get(0).getRuleId(), equalTo(HealthRuleIds.DEAD_WIKI_LINKS));
    assertThat(group.getChildren().get(0).isAutoFixable(), equalTo(false));
    assertThat(group.getChildren().get(0).getSeverity(), equalTo(HealthSeverity.warning));
  }

  @Test
  void alwaysEmitsDeadWikiLinksGroupWithMetadataWhenEmpty() {
    HealthFindingGroup group = deadWikiLinksGroup();

    assertThat(group, is(not(nullValue())));
    assertThat(group.getRuleId(), equalTo(HealthRuleIds.DEAD_WIKI_LINKS));
    assertThat(group.getTitle(), equalTo("Dead wiki links"));
    assertThat(group.getSeverity(), equalTo(HealthSeverity.warning));
    assertThat(group.isAutoFixable(), equalTo(false));
    assertThat(group.getChildren(), is(not(nullValue())));
    assertThat(group.getChildren(), empty());
    assertThat(group.getItems() == null || group.getItems().isEmpty(), is(true));
  }

  @Test
  void oneLintReportIncludesFolderGroupsAndDeadWikiLinks() {
    makeMe.aFolder().notebook(notebook).name("Empty").please();
    makeMe.aNote().title("Source").notebook(notebook).content("[[Missing]]").please();

    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));
    List<String> ruleIds = report.getGroups().stream().map(HealthFindingGroup::getRuleId).toList();

    assertThat(ruleIds, hasItem(HealthRuleIds.EMPTY_FOLDERS));
    assertThat(ruleIds, hasItem(HealthRuleIds.README_ONLY_FOLDERS));
    assertThat(ruleIds, hasItem(HealthRuleIds.DEAD_WIKI_LINKS));
  }

  @Test
  void lintDoesNotMutateNoteContentOrCount() {
    Note source =
        makeMe.aNote().title("Source").notebook(notebook).content("See [[Missing]]").please();
    String contentBefore = source.getContent();
    int liveCountBefore =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).size();

    notebookHealthService.lint(notebook, new HealthRunContext(owner));
    makeMe.entityPersister.refresh(source);

    assertThat(source.getContent(), equalTo(contentBefore));
    assertThat(
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()),
        hasSize(liveCountBefore));
  }

  private HealthFindingGroup deadWikiLinksGroup() {
    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));
    List<HealthFindingGroup> groups = report.getGroups();
    assertThat(groups, is(not(nullValue())));
    return groups.stream()
        .filter(g -> HealthRuleIds.DEAD_WIKI_LINKS.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing dead_wiki_links group"));
  }

  private static HealthFindingGroup soleChild(HealthFindingGroup group) {
    assertThat(group.getChildren(), hasSize(1));
    return group.getChildren().getFirst();
  }
}
