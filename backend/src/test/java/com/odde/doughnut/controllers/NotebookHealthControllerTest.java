package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.NotebookHealthFixRequest;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.health.HealthRuleIds;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

  @Nested
  class FixHealth {
    @Test
    void authorizedOwnerFixSucceeds() throws UnexpectedNoAccessRightException {
      Notebook notebook = myNotebook();
      Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();
      Folder readmeOnly =
          makeMe.aFolder().notebook(notebook).name("Readme Only").readmeContent("keep").please();

      controller.fix(notebook, fixRequest(true));

      Set<Integer> remainingIds =
          folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
              .map(Folder::getId)
              .collect(Collectors.toSet());
      assertThat(remainingIds, not(hasItem(emptyFolder.getId())));
      assertThat(remainingIds, hasItem(readmeOnly.getId()));
    }

    @Test
    void fixRejectsWithoutOptIn() {
      Notebook notebook = myNotebook();
      Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();

      ResponseStatusException nullBody =
          assertThrows(
              ResponseStatusException.class, () -> controller.fix(notebook, fixRequest(null)));
      assertThat(nullBody.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

      ResponseStatusException falseBody =
          assertThrows(
              ResponseStatusException.class, () -> controller.fix(notebook, fixRequest(false)));
      assertThat(falseBody.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

      assertThat(
          folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
              .map(Folder::getId)
              .toList(),
          hasItem(emptyFolder.getId()));
    }

    @Test
    void foreignAndAnonRejected() {
      Notebook otherNotebook = otherUsersNotebook();
      makeMe.aFolder().notebook(otherNotebook).name("Empty Shell").please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.fix(otherNotebook, fixRequest(true)));

      Notebook notebook = myNotebook();
      makeMe.aFolder().notebook(notebook).name("Empty Shell").please();
      currentUser.setUser(null);

      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.fix(notebook, fixRequest(true)));
    }

    private NotebookHealthFixRequest fixRequest(Boolean removeEmptyFolders) {
      NotebookHealthFixRequest request = new NotebookHealthFixRequest();
      request.setRemoveEmptyFolders(removeEmptyFolders);
      return request;
    }
  }
}
