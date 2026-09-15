package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebTrashGuardControllerTest extends NotebookGitWebContentControllerTestBase {
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String OCCUPIER_BODY = "---\ntype: Note\n---\noccupier";

  @Autowired RelationController relationController;

  @Test
  void unauthorizedTrashLeavesLocationContentAndAcceptedBindingUnchanged() throws Exception {
    LiveCellsFixture f = seedLiveCellsInBiology();
    CommittedNoteAndBinding setup =
        committedNoteAndBinding(f.cells().getId(), f.notebook().getId());
    User otherUser = createFixtureUser();
    currentUser.setUser(otherUser);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> noteController.trashNote(f.cells(), leaveDeadLinks()));

    CommittedNoteAndBinding committed =
        committedNoteAndBinding(f.cells().getId(), f.notebook().getId());
    assertThat(committed.folderId(), equalTo(f.biology().getId()));
    assertThat(committed.content(), equalTo(CELLS_BODY));
    assertThat(committed.trashed(), is(false));
    assertThat(committed.acceptedHead(), equalTo(setup.acceptedHead()));
    assertThat(committed.acceptedBundle(), equalTo(setup.acceptedBundle()));
  }

  @Test
  void occupiedMoveRecoveryOfGitTrashedNoteLeavesNoteInTrashAndAcceptedBindingUnchanged()
      throws Exception {
    OccupiedRecoveryFixture f = seedGitTrashedCellsWithOccupiedBiology();
    CommittedNoteAndBinding setup =
        committedNoteAndBinding(f.cells().getId(), f.notebook().getId());

    ApiException conflict =
        assertThrows(
            ApiException.class, () -> relationController.moveNoteToFolder(f.cells(), f.biology()));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertOccupiedRecoveryUnchanged(f, setup);
  }

  @Test
  void occupiedUndoOfGitTrashedNoteLeavesNoteInTrashAndAcceptedBindingUnchanged() throws Exception {
    OccupiedRecoveryFixture f = seedGitTrashedCellsWithOccupiedBiology();
    CommittedNoteAndBinding setup =
        committedNoteAndBinding(f.cells().getId(), f.notebook().getId());

    ApiException conflict =
        assertThrows(
            ApiException.class,
            () -> noteController.undoTrashNote(f.cells(), undoTo("Cells", f.biology())));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertOccupiedRecoveryUnchanged(f, setup);
  }

  void assertOccupiedRecoveryUnchanged(OccupiedRecoveryFixture f, CommittedNoteAndBinding setup) {
    inCommittedTransaction(
        transactionManager,
        () -> {
          Note cells = noteRepository.findById(f.cells().getId()).orElseThrow();
          Note occupier = noteRepository.findById(f.occupier().getId()).orElseThrow();
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(f.notebook().getId()).orElseThrow();
          assertThat(cells.getFolder().getId(), equalTo(f.trashBiology().getId()));
          assertThat(cells.getContent(), equalTo(CELLS_BODY));
          assertThat(cells.isTrashed(), is(true));
          assertThat(occupier.getFolder().getId(), equalTo(f.biology().getId()));
          assertThat(occupier.getContent(), equalTo(OCCUPIER_BODY));
          assertThat(binding.getAcceptedGitObjectId(), equalTo(setup.acceptedHead()));
          assertThat(binding.getBundleBytes(), equalTo(setup.acceptedBundle()));
        });
  }

  LiveCellsFixture seedLiveCellsInBiology() throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new LiveCellsFixture(notebook, biology, cells);
  }

  OccupiedRecoveryFixture seedGitTrashedCellsWithOccupiedBiology()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder trashBiology = makeMe.aFolder().parentFolder(trash).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(trashBiology).content(CELLS_BODY).please();
    Note occupier = makeMe.aNote("Cells").folder(biology).content(OCCUPIER_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new OccupiedRecoveryFixture(notebook, biology, trashBiology, cells, occupier);
  }

  CommittedNoteAndBinding committedNoteAndBinding(Integer noteId, Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Note note = noteRepository.findById(noteId).orElseThrow();
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
          return new CommittedNoteAndBinding(
              note.getFolder().getId(),
              note.getContent(),
              note.isTrashed(),
              binding.getAcceptedGitObjectId(),
              binding.getBundleBytes().clone());
        });
  }

  record LiveCellsFixture(Notebook notebook, Folder biology, Note cells) {}

  record OccupiedRecoveryFixture(
      Notebook notebook, Folder biology, Folder trashBiology, Note cells, Note occupier) {}

  record CommittedNoteAndBinding(
      Integer folderId,
      String content,
      boolean trashed,
      String acceptedHead,
      byte[] acceptedBundle) {}
}
