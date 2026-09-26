package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Slice 2 guard variants: destination collision, denied access, same-place no-op, and non-Git
 * behavior for web note moves. Each asserts the move leaves placement, content, and the accepted
 * binding unchanged when rejected, and that non-Git notebooks move without creating a binding.
 */
class NotebookGitWebNoteMoveGuardControllerTest extends NotebookGitWebNoteMoveTestBase {

  @Test
  void destinationCollisionRejectsMoveAndLeavesPlacementContentAndBindingUnchanged()
      throws Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    Note occupier = makeMe.aNote("Cells").folder(f.study()).content("occupier").please();

    ApiException conflict =
        assertThrows(
            ApiException.class, () -> relationController.moveNoteToFolder(f.cells(), f.study()));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    Note cellsAfter = reloadNote(f.cells());
    Note occupierAfter = reloadNote(occupier);
    assertThat(cellsAfter.getFolder().getId(), equalTo(f.biology().getId()));
    assertThat(cellsAfter.getContent(), equalTo(CELLS_BODY));
    assertThat(occupierAfter.getFolder().getId(), equalTo(f.study().getId()));
    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void aFolderHoldingTheNoteFileNameIgnoringCaseRefusesTheMoveNamingItAndChangesNothing()
      throws Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    makeMe.aFolder().parentFolder(f.study()).name("cells.md").please();

    ApiException conflict =
        assertThrows(
            ApiException.class, () -> relationController.moveNoteToFolder(f.cells(), f.study()));

    ApiError error = conflict.getErrorBody();
    assertThat(error.getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(error.getErrors().get("newTitle"), containsString("Study/cells.md"));
    assertThat(reloadNote(f.cells()).getFolder().getId(), equalTo(f.biology().getId()));
    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void deniedAccessLeavesPlacementContentAndBindingUnchanged() throws Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    User otherUser = createFixtureUser();
    currentUser.setUser(otherUser);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> relationController.moveNoteToFolder(f.cells(), f.study()));

    currentUser.setUser(f.owner());
    Note cellsAfter = reloadNote(f.cells());
    assertThat(cellsAfter.getFolder().getId(), equalTo(f.biology().getId()));
    assertThat(cellsAfter.getContent(), equalTo(CELLS_BODY));
    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void samePlaceMoveIsNoOpAndLeavesAcceptedHeadUnchanged() throws Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));

    relationController.moveNoteToFolder(f.cells(), f.biology());

    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedA));
    assertThat(reloadNote(f.cells()).getFolder().getId(), equalTo(f.biology().getId()));
  }

  @Test
  void nonGitNotebookMoveStillWorksAndCreatesNoBinding() throws UnexpectedNoAccessRightException {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder source = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder dest = makeMe.aFolder().notebook(notebook).name("Study").please();
    Note mover = makeMe.aNote("Cells").folder(source).content("body").please();

    relationController.moveNoteToFolder(mover, dest);

    assertThat(reloadNote(mover).getFolder().getId(), equalTo(dest.getId()));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isPresent(), is(false));
  }
}
