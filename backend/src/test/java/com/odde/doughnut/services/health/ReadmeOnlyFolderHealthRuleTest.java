package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.services.NotebookHealthService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReadmeOnlyFolderHealthRuleTest {
  @Autowired NotebookHealthService notebookHealthService;
  @Autowired FolderRepository folderRepository;
  @Autowired MakeMe makeMe;

  private User owner;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    owner = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
  }

  @Test
  void listsEveryNestedNoteEmptyFolderWithNonBlankOwnReadme() {
    Folder parent =
        makeMe.aFolder().notebook(notebook).name("Parent").readmeContent("parent readme").please();
    Folder child =
        makeMe.aFolder().parentFolder(parent).name("Child").readmeContent("child readme").please();

    HealthFindingGroup group = readmeOnlyFoldersGroup();

    assertThat(
        group.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(parent.getId(), child.getId()));
    assertThat(
        group.getItems().stream().map(HealthFindingItem::getLabel).toList(),
        containsInAnyOrder("Parent", "Child"));
  }

  @Test
  void liveNoteInDescendantClearsOccupancyFromBothGroups() {
    Folder parent =
        makeMe.aFolder().notebook(notebook).name("Parent").readmeContent("parent readme").please();
    Folder child =
        makeMe.aFolder().parentFolder(parent).name("Child").readmeContent("child readme").please();
    makeMe.aNote("live").folder(child).please();

    assertThat(readmeOnlyFoldersGroup().getItems(), empty());
    assertThat(emptyFoldersGroup().getItems(), empty());
  }

  @Test
  void softDeletedNoteDoesNotOccupyReadmeOnlyFolder() {
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("OnlyDeleted")
            .readmeContent("still here")
            .please();
    makeMe.aNote("gone").folder(folder).softDeleted().please();

    HealthFindingGroup group = readmeOnlyFoldersGroup();

    assertThat(group.getItems(), hasSize(1));
    assertThat(group.getItems().get(0).getFolderId(), equalTo(folder.getId()));
    assertThat(group.getItems().get(0).getLabel(), equalTo("OnlyDeleted"));
  }

  @Test
  void partitionsByOwnReadmeBlanknessWithMutualExclusion() {
    Folder withReadme =
        makeMe.aFolder().notebook(notebook).name("HasReadme").readmeContent("keep me").please();
    Folder blankReadme =
        makeMe.aFolder().notebook(notebook).name("BlankReadme").readmeContent("   ").please();
    Folder nullReadme = makeMe.aFolder().notebook(notebook).name("NullReadme").please();

    HealthFindingGroup readmeOnly = readmeOnlyFoldersGroup();
    HealthFindingGroup empty = emptyFoldersGroup();

    assertThat(
        readmeOnly.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(withReadme.getId()));
    assertThat(
        empty.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(blankReadme.getId(), nullReadme.getId()));

    Set<Integer> readmeOnlyIds = folderIds(readmeOnly);
    Set<Integer> emptyIds = folderIds(empty);
    assertThat(readmeOnlyIds.stream().filter(emptyIds::contains).toList(), empty());
  }

  @Test
  void ownReadmeOnlyDoesNotInheritFromParent() {
    Folder parent =
        makeMe.aFolder().notebook(notebook).name("Parent").readmeContent("parent readme").please();
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();

    HealthFindingGroup readmeOnly = readmeOnlyFoldersGroup();
    HealthFindingGroup empty = emptyFoldersGroup();

    assertThat(
        readmeOnly.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(parent.getId()));
    assertThat(
        empty.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
        containsInAnyOrder(child.getId()));
    assertThat(folderIds(readmeOnly), not(hasItem(child.getId())));
    assertThat(folderIds(empty), not(hasItem(parent.getId())));
  }

  @Test
  void alwaysEmitsReadmeOnlyFoldersGroupWithMetadata() {
    HealthFindingGroup group = readmeOnlyFoldersGroup();

    assertThat(group, is(not(nullValue())));
    assertThat(group.getRuleId(), equalTo(HealthRuleIds.README_ONLY_FOLDERS));
    assertThat(group.getTitle(), equalTo("Readme-only folders"));
    assertThat(group.getSeverity(), equalTo(HealthSeverity.warning));
    assertThat(group.isAutoFixable(), equalTo(false));
    assertThat(group.getItems(), is(not(nullValue())));
    assertThat(group.getItems(), empty());
  }

  @Test
  void oneLintReportIncludesBothEmptyAndReadmeOnlyGroups() {
    makeMe.aFolder().notebook(notebook).name("HasReadme").readmeContent("keep me").please();
    makeMe.aFolder().notebook(notebook).name("Empty").please();

    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));
    List<String> ruleIds = report.getGroups().stream().map(HealthFindingGroup::getRuleId).toList();

    assertThat(ruleIds, hasItem(HealthRuleIds.EMPTY_FOLDERS));
    assertThat(ruleIds, hasItem(HealthRuleIds.README_ONLY_FOLDERS));
  }

  @Test
  void lintDoesNotChangeFolderCount() {
    makeMe.aFolder().notebook(notebook).name("HasReadme").readmeContent("keep me").please();
    makeMe.aFolder().notebook(notebook).name("Empty").please();
    int folderCountBefore = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).size();

    notebookHealthService.lint(notebook, new HealthRunContext(owner));

    assertThat(
        folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()),
        hasSize(folderCountBefore));
  }

  private HealthFindingGroup readmeOnlyFoldersGroup() {
    return groupByRuleId(HealthRuleIds.README_ONLY_FOLDERS);
  }

  private HealthFindingGroup emptyFoldersGroup() {
    return groupByRuleId(HealthRuleIds.EMPTY_FOLDERS);
  }

  private HealthFindingGroup groupByRuleId(String ruleId) {
    NotebookHealthLintReport report =
        notebookHealthService.lint(notebook, new HealthRunContext(owner));
    List<HealthFindingGroup> groups = report.getGroups();
    assertThat(groups, is(not(nullValue())));
    return groups.stream()
        .filter(g -> ruleId.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing " + ruleId + " group"));
  }

  private static Set<Integer> folderIds(HealthFindingGroup group) {
    return group.getItems().stream()
        .map(HealthFindingItem::getFolderId)
        .collect(Collectors.toSet());
  }
}
