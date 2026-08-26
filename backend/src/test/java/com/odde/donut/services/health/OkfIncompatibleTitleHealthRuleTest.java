package com.odde.donut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.HealthSeverity;
import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.NotebookHealthService;
import com.odde.donut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @Test
  void listsNotesWhoseTitlesOccupyOkfReservedBasenames() {
    Note note = makeMe.aNote("INDEX").notebook(notebook).please();

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

  private HealthFindingGroup okfIncompatibleTitlesGroup() {
    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));
    return report.getGroups().stream()
        .filter(g -> HealthRuleIds.OKF_INCOMPATIBLE_TITLES.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError("missing " + HealthRuleIds.OKF_INCOMPATIBLE_TITLES + " group"));
  }
}
