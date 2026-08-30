package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.NotebookHealthFixRequest;
import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.health.HealthRuleIds;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookHealthControllerTest extends ControllerTestBase {

  @Autowired NotebookHealthController controller;
  @Autowired FolderRepository folderRepository;
  @Autowired TextContentController textContentController;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook ownedNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private Notebook otherUsersNotebook() {
    return makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
  }

  private HealthFindingGroup emptyFoldersGroup(NotebookHealthLintReport report) {
    return healthGroup(report, HealthRuleIds.EMPTY_FOLDERS);
  }

  private HealthFindingGroup deadWikiLinksGroup(NotebookHealthLintReport report) {
    return healthGroup(report, HealthRuleIds.DEAD_WIKI_LINKS);
  }

  private HealthFindingGroup healthGroup(NotebookHealthLintReport report, String ruleId) {
    return report.getGroups().stream()
        .filter(g -> ruleId.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow();
  }

  private NotebookHealthFixRequest fixRequest(Boolean removeEmptyFolders) {
    NotebookHealthFixRequest request = new NotebookHealthFixRequest();
    request.setRemoveEmptyFolders(removeEmptyFolders);
    return request;
  }

  @Nested
  class LintHealth {
    @Test
    void ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();
      int folderCountBefore =
          folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).size();

      HealthFindingGroup group = emptyFoldersGroup(controller.lint(notebook));
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
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.lint(otherUsersNotebook()));
    }

    @Test
    void rejectsAnonymousUser() {
      Notebook notebook = ownedNotebook();
      currentUser.setUser(null);
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(notebook));
    }
  }

  @Nested
  class FixHealth {
    @Test
    void authorizedOwnerFixSucceeds() throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
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

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void fixRejectsWithoutOptIn(Boolean removeEmptyFolders) {
      Notebook notebook = ownedNotebook();
      Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();

      ResponseStatusException ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.fix(notebook, fixRequest(removeEmptyFolders)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(
          folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
              .map(Folder::getId)
              .toList(),
          hasItem(emptyFolder.getId()));
    }

    @Test
    void foreignRejected() {
      Notebook otherNotebook = otherUsersNotebook();
      makeMe.aFolder().notebook(otherNotebook).name("Empty Shell").please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.fix(otherNotebook, fixRequest(true)));
    }

    @Test
    void anonymousRejected() {
      Notebook notebook = ownedNotebook();
      makeMe.aFolder().notebook(notebook).name("Empty Shell").please();
      currentUser.setUser(null);
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.fix(notebook, fixRequest(true)));
    }
  }

  @Nested
  class DeadPropertyWikiLinks {
    @ParameterizedTest
    @ValueSource(strings = {"Moon#prop:a%20part%20of", "Moon#prop:%ZZ", "Moon#prop:wikidata"})
    void reportsAbsentInvalidOrMismatchedPropertyTokens(String token)
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      makeMe
          .aNote()
          .title("Moon")
          .notebook(notebook)
          .content(Frontmatter.empty().set("WikiData", "v").fenced(""))
          .please();
      makeMe.aNote().title("Linker").notebook(notebook).content("[[" + token + "]]").please();

      HealthFindingGroup group = deadWikiLinksGroup(controller.lint(notebook));
      assertThat(group.getChildren(), hasSize(1));
      assertThat(
          group.getChildren().getFirst().getItems().getFirst().getWikiLinkToken(), equalTo(token));
    }

    @Test
    void doesNotReportLivePropertyToken() throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      makeMe
          .aNote()
          .title("Moon")
          .notebook(notebook)
          .content(Frontmatter.empty().set("a part of", "v").fenced(""))
          .please();
      makeMe
          .aNote()
          .title("Linker")
          .notebook(notebook)
          .content("[[Moon#prop:a%20part%20of]]")
          .please();

      assertThat(deadWikiLinksGroup(controller.lint(notebook)).getChildren(), empty());
    }

    @Test
    void reportsPropertyTokenAfterTargetPropertyIsRemoved()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Note moon =
          makeMe
              .aNote()
              .title("Moon")
              .notebook(notebook)
              .content(Frontmatter.empty().set("a part of", "v").fenced(""))
              .please();
      Note linker = makeMe.aNote().title("Linker").notebook(notebook).please();
      textContentController.updateNoteContent(linker, contentDto("[[Moon#prop:a%20part%20of]]"));

      textContentController.updateNoteContent(moon, contentDto("Moon body."));

      HealthFindingGroup group = deadWikiLinksGroup(controller.lint(notebook));
      assertThat(group.getChildren(), hasSize(1));
      assertThat(
          group.getChildren().getFirst().getItems().getFirst().getWikiLinkToken(),
          equalTo("Moon#prop:a%20part%20of"));
    }
  }

  private NoteUpdateContentDTO contentDto(String content) {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    return dto;
  }
}
