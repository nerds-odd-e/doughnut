package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A note moved within its notebook carries the file its {@code image:} names in its folder. */
class NotebookGitWebNoteMovePictureControllerTest extends NotebookGitWebNoteMoveTestBase {
  static final String FORCE_BODY = "---\nimage: force.png\n---\nforce body";

  Notebook notebook;
  Folder physics;
  Folder mechanics;
  Note force;
  NotebookAttachment picture;
  byte[] pointer;

  @BeforeEach
  void seedForceWithItsPicture() throws Exception {
    notebook = createGitBackedNotebook();
    physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    mechanics = makeMe.aFolder().notebook(notebook).name("mechanics").please();
    force = makeMe.aNote("Force").folder(physics).content(FORCE_BODY).please();
    picture = storeFolderAttachmentAndSnapshot(notebook, physics, "force.png", new byte[] {7});
    pointer = picture.getAcceptedGitContent();
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));
  }

  @Test
  void movingIntoAFolderCarriesThePictureInOneAcceptedCommit() throws Exception {
    AcceptedHistory before = acceptedHistory(notebook);

    relationController.moveNoteToFolder(force, mechanics);

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(
        after.tipPaths(),
        containsInAnyOrder("physics/.keep", "mechanics/Force.md", "mechanics/force.png"));
    assertThat(tipText(after, "mechanics/Force.md"), equalTo(FORCE_BODY));
    assertThat(tipContent(after, "mechanics/force.png"), equalTo(pointer));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void movingToTheNotebookRootCarriesThePicture() throws Exception {
    relationController.moveNoteToNotebookRoot(force);

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(
        after.tipPaths(),
        containsInAnyOrder("physics/.keep", "mechanics/.keep", "Force.md", "force.png"));
    assertThat(tipContent(after, "force.png"), equalTo(pointer));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void aSamePlaceMoveKeepsThePicture() throws Exception {
    relationController.moveNoteToFolder(force, physics);

    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder("physics/Force.md", "physics/force.png", "mechanics/.keep"));
  }

  @Test
  void aDestinationEntryHoldingThePictureNameRefusesTheMoveAndChangesNothing() throws Exception {
    storeFolderAttachmentAndSnapshot(notebook, mechanics, "Force.png", new byte[] {9});
    String acceptedBefore = binding(notebook).getAcceptedGitObjectId();

    ApiException conflict =
        assertThrows(
            ApiException.class, () -> relationController.moveNoteToFolder(force, mechanics));

    ApiError error = conflict.getErrorBody();
    assertThat(error.getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(error.getMessage(), containsString("mechanics/Force.png"));
    assertThat(binding(notebook).getAcceptedGitObjectId(), equalTo(acceptedBefore));
    assertThat(reloadNote(force).getFolder().getId(), equalTo(physics.getId()));
    assertThat(
        notebookAttachmentRepository.findById(picture.getId()).orElseThrow().getFolder().getId(),
        equalTo(physics.getId()));
  }
}
