package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.health.HealthRuleIds;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookHealthControllerTest extends ControllerTestBase {

  @Autowired NotebookHealthController controller;
  @Autowired FolderRepository folderRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook myNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private Notebook otherUsersNotebook() {
    return makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
  }

  private HealthFindingGroup emptyFoldersGroup(NotebookHealthLintReport report) {
    assertThat(report.getGroups(), is(not(nullValue())));
    return report.getGroups().stream()
        .filter(g -> HealthRuleIds.EMPTY_FOLDERS.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow();
  }

  @Nested
  class LintHealth {
    @Test
    void ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = myNotebook();
      Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();
      int folderCountBefore =
          folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).size();

      NotebookHealthLintReport report = controller.lint(notebook);

      HealthFindingGroup group = emptyFoldersGroup(report);
      assertThat(group.getRuleId(), equalTo(HealthRuleIds.EMPTY_FOLDERS));
      assertThat(
          group.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
          hasItem(emptyFolder.getId()));
      assertThat(
          group.getItems().stream().map(HealthFindingItem::getLabel).toList(),
          hasItem("Empty Shell"));

      List<Folder> foldersAfter = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
      assertThat(foldersAfter, hasSize(folderCountBefore));
    }

    @Test
    void rejectsForeignUser() {
      Notebook otherNotebook = otherUsersNotebook();

      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(otherNotebook));
    }

    @Test
    void rejectsAnonymousUser() {
      Notebook notebook = myNotebook();
      currentUser.setUser(null);

      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(notebook));
    }
  }
}
