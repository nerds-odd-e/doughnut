package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.NotebookHealthService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmptyFolderHealthRuleTest {
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
  void listsEveryNestedFullyEmptyFolder() {
    Folder parent = makeMe.aFolder().notebook(notebook).name("Parent").please();
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();

    HealthFindingGroup group = emptyFoldersGroup();

    assertThat(
        group.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(parent.getId(), child.getId()));
    assertThat(
        group.getItems().stream().map(HealthFindingItem::getLabel).toList(),
        containsInAnyOrder("Parent", "Child"));
  }

  @Test
  void liveNoteInDescendantClearsAncestorOccupancy() {
    Folder parent = makeMe.aFolder().notebook(notebook).name("Parent").please();
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();
    makeMe.aNote("live").folder(child).please();

    assertThat(emptyFoldersGroup().getItems(), empty());
  }

  @Test
  void softDeletedNoteDoesNotOccupyFolder() {
    Folder folder = makeMe.aFolder().notebook(notebook).name("OnlyDeleted").please();
    makeMe.aNote("gone").folder(folder).softDeleted().please();

    assertThat(
        emptyFoldersGroup().getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(folder.getId()));
  }

  @Test
  void nonBlankReadmeExcludesFolderFromEmptyFolders() {
    makeMe.aFolder().notebook(notebook).name("HasReadme").readmeContent("keep me").please();
    Folder blankReadme =
        makeMe.aFolder().notebook(notebook).name("BlankReadme").readmeContent("   ").please();
    Folder nullReadme = makeMe.aFolder().notebook(notebook).name("NullReadme").please();

    assertThat(
        emptyFoldersGroup().getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(blankReadme.getId(), nullReadme.getId()));
  }

  @Test
  void alwaysEmitsEmptyFoldersGroupWithMetadata() {
    HealthFindingGroup group = emptyFoldersGroup();

    assertThat(group.getRuleId(), equalTo(HealthRuleIds.EMPTY_FOLDERS));
    assertThat(group.getTitle(), equalTo("Empty folders"));
    assertThat(group.getSeverity(), equalTo(HealthSeverity.warning));
    assertThat(group.isAutoFixable(), equalTo(true));
    assertThat(group.getItems(), empty());
  }

  private HealthFindingGroup emptyFoldersGroup() {
    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));
    return report.getGroups().stream()
        .filter(g -> HealthRuleIds.EMPTY_FOLDERS.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing empty_folders group"));
  }
}
