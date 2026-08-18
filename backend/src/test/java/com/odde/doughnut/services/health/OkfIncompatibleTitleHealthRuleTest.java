package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.NotebookHealthService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OkfIncompatibleTitleHealthRuleTest {
  @Autowired NotebookHealthService notebookHealthService;
  @Autowired MakeMe makeMe;

  private User owner;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    owner = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
  }

  @Test
  void alwaysEmitsOkfIncompatibleTitlesGroupWithMetadataWhenEmpty() {
    HealthFindingGroup group = okfIncompatibleTitlesGroup();

    assertThat(group.getRuleId(), equalTo(HealthRuleIds.OKF_INCOMPATIBLE_TITLES));
    assertThat(group.getTitle(), equalTo("OKF-incompatible titles"));
    assertThat(group.getSeverity(), equalTo(HealthSeverity.warning));
    assertThat(group.isAutoFixable(), equalTo(false));
    assertThat(group.getItems(), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"index", "INDEX", "index.md", "log", "LOG.MD"})
  void listsNotesWhoseTitlesOccupyOkfReservedBasenames(String title) {
    Note note = makeMe.aNote(title).notebook(notebook).please();

    HealthFindingGroup group = okfIncompatibleTitlesGroup();
    assertThat(
        group.getItems().stream().map(HealthFindingItem::getNoteId).toList(),
        containsInAnyOrder(note.getId()));
    assertThat(
        group.getItems().stream().map(HealthFindingItem::getLabel).toList(),
        containsInAnyOrder(note.getTitle()));
  }

  @Test
  void doesNotListOrdinaryNoteTitles() {
    makeMe.aNote("indexical").notebook(notebook).please();

    assertThat(okfIncompatibleTitlesGroup().getItems(), empty());
  }

  @Test
  void excludesSoftDeletedNotes() {
    makeMe.aNote("index").notebook(notebook).softDeleted().please();
    Note live = makeMe.aNote("log").notebook(notebook).please();

    assertThat(
        okfIncompatibleTitlesGroup().getItems().stream().map(HealthFindingItem::getNoteId).toList(),
        containsInAnyOrder(live.getId()));
  }

  @Test
  void emptyFolderAndDeadWikiGroupsStillReportWhenOkfTitleExists() {
    Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();
    makeMe.aNote("index").notebook(notebook).content("See [[Missing]]").please();

    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));

    assertThat(
        groupFrom(report, HealthRuleIds.EMPTY_FOLDERS).getItems().stream()
            .map(HealthFindingItem::getFolderId)
            .toList(),
        hasItem(emptyFolder.getId()));
    assertThat(groupFrom(report, HealthRuleIds.DEAD_WIKI_LINKS).getChildren(), not(empty()));
  }

  private HealthFindingGroup okfIncompatibleTitlesGroup() {
    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));
    return groupFrom(report, HealthRuleIds.OKF_INCOMPATIBLE_TITLES);
  }

  private static HealthFindingGroup groupFrom(NotebookHealthLintReport report, String ruleId) {
    return report.getGroups().stream()
        .filter(g -> ruleId.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing " + ruleId + " group"));
  }
}
