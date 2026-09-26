package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.HealthSeverity;
import com.odde.donut.controllers.dto.NotebookHealthFixRequest;
import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
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

  private HealthFindingGroup healthGroup(NotebookHealthLintReport report, String ruleId) {
    return report.getGroups().stream()
        .filter(g -> ruleId.equals(g.getRuleId()))
        .findFirst()
        .orElseThrow();
  }

  private Note storedNoteInTrashSubfolder(Notebook notebook) {
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder kept = makeMe.aFolder().parentFolder(trash).name("kept").please();
    return makeMe.aNote().folder(kept).please();
  }

  private Set<Integer> folderIds(Notebook notebook) {
    return folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
        .map(Folder::getId)
        .collect(Collectors.toSet());
  }

  private NotebookHealthFixRequest fixRequest(Boolean removeEmptyFolders) {
    NotebookHealthFixRequest request = new NotebookHealthFixRequest();
    request.setRemoveEmptyFolders(removeEmptyFolders);
    return request;
  }

  @Nested
  class LintHealth {
    @Test
    void reportsPipeTitleAndAliasCompatibilityPerAffectedNote()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Note titleAndAlias =
          makeMe
              .aNote("Title||Pipe")
              .notebook(notebook)
              .content("---\naliases:\n  - Alias||Pipe\n---\n\nBody")
              .please();
      Note aliasOnly =
          makeMe
              .aNote("Alias only")
              .notebook(notebook)
              .content("---\naliases:\n  - Other|Name\n---\n\nBody")
              .please();

      HealthFindingGroup group =
          healthGroup(controller.lint(notebook), HealthRuleIds.PIPE_NAME_COMPATIBILITY);

      assertThat(group.getSeverity(), equalTo(HealthSeverity.warning));
      assertThat(group.isAutoFixable(), equalTo(false));
      assertThat(
          group.getItems().stream().map(HealthFindingItem::getNoteId).toList(),
          contains(titleAndAlias.getId(), aliasOnly.getId()));
      assertThat(
          group.getItems().stream().map(HealthFindingItem::getMessage).toList(),
          contains(
              "The title and an alias contain pipes, so its filename is not Windows-compatible and references may not work in other Markdown tools.",
              "An alias contains a pipe, so references may not work in other Markdown tools."));
    }

    @Test
    void ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();
      Set<Integer> folderIdsBefore = folderIds(notebook);

      HealthFindingGroup group = emptyFoldersGroup(controller.lint(notebook));
      assertThat(
          group.getItems().stream().map(HealthFindingItem::getFolderId).toList(),
          hasItem(emptyFolder.getId()));
      assertThat(
          group.getItems().stream().map(HealthFindingItem::getLabel).toList(),
          hasItem("Empty Shell"));

      assertThat(folderIds(notebook), equalTo(folderIdsBefore));
    }

    @Test
    void storedTrashNoteKeepsItsFolderChainOutOfEmptyFolderFindings()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Folder kept = storedNoteInTrashSubfolder(notebook).getFolder();
      Folder emptySibling = makeMe.aFolder().notebook(notebook).name("Empty Shell").please();

      List<Integer> reportedFolderIds =
          emptyFoldersGroup(controller.lint(notebook)).getItems().stream()
              .map(HealthFindingItem::getFolderId)
              .toList();

      assertThat(reportedFolderIds, contains(emptySibling.getId()));
      assertThat(reportedFolderIds, not(hasItem(kept.getId())));
      assertThat(reportedFolderIds, not(hasItem(kept.getParentFolder().getId())));
    }

    @Test
    void folderHoldingOnlyAFileIsNotReportedEmpty() throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Folder refs = makeMe.aFolder().notebook(notebook).name("refs").please();
      makeMe.anAttachment("paper.pdf").in(refs).please();

      assertThat(
          emptyFoldersGroup(controller.lint(notebook)).getItems().stream()
              .map(HealthFindingItem::getFolderId)
              .toList(),
          not(hasItem(refs.getId())));
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

      assertThat(folderIds(notebook), not(hasItem(emptyFolder.getId())));
      assertThat(folderIds(notebook), hasItem(readmeOnly.getId()));
    }

    @Test
    void fixLeavesOccupiedTrashFoldersAndTheirStoredNoteInPlace()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Note storedNote = storedNoteInTrashSubfolder(notebook);
      Integer keptId = storedNote.getFolder().getId();
      Integer trashId = storedNote.getFolder().getParentFolder().getId();
      makeMe.aFolder().notebook(notebook).name("Empty Shell").please();

      controller.fix(notebook, fixRequest(true));
      makeMe.entityPersister.flushAndClear();

      assertThat(folderIds(notebook), containsInAnyOrder(trashId, keptId));
      Note refound = makeMe.entityPersister.find(Note.class, storedNote.getId());
      assertThat(refound.getFolder().getId(), equalTo(keptId));
    }

    @Test
    void fixLeavesAFolderHoldingOnlyAFileAndItsFileInPlace()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = ownedNotebook();
      Folder refs = makeMe.aFolder().notebook(notebook).name("refs").please();
      NotebookAttachment paper = makeMe.anAttachment("paper.pdf").in(refs).please();

      controller.fix(notebook, fixRequest(true));
      makeMe.entityPersister.flushAndClear();

      assertThat(folderIds(notebook), contains(refs.getId()));
      NotebookAttachment refound =
          makeMe.entityPersister.find(NotebookAttachment.class, paper.getId());
      assertThat(refound.getFolder().getId(), equalTo(refs.getId()));
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
      assertThat(folderIds(notebook), hasItem(emptyFolder.getId()));
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
}
