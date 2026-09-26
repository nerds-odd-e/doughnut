package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A note moved within its notebook carries the file its {@code image:} names in its folder. */
class NotebookGitWebNoteMovePictureControllerTest extends NotebookGitWebNoteMoveTestBase {
  static final String FORCE_BODY = "---\nimage: force.png\n---\nforce body";
  static final String ENERGY_BODY = "---\nimage: force.png\n---\nenergy body";

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
  void aTakenNameGivesThePictureTheFirstFreeNameAndRewritesItsImage() throws Exception {
    NotebookAttachment taken =
        storeFolderAttachmentAndSnapshot(notebook, mechanics, "Force.png", new byte[] {9});
    AcceptedHistory before = acceptedHistory(notebook);

    relationController.moveNoteToFolder(force, mechanics);

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(
        after.tipPaths(),
        containsInAnyOrder(
            "physics/.keep",
            "mechanics/Force.md",
            "mechanics/Force.png",
            "mechanics/force (2).png"));
    assertThat(
        tipText(after, "mechanics/Force.md"),
        equalTo("---\nimage: force (2).png\n---\nforce body"));
    assertThat(tipContent(after, "mechanics/force (2).png"), equalTo(pointer));
    assertThat(tipContent(after, "mechanics/Force.png"), equalTo(taken.getAcceptedGitContent()));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void aPictureAnotherNoteUsesIsCopiedNotMoved() throws Exception {
    makeMe.aNote("Energy").folder(physics).content(ENERGY_BODY).please();
    snapshotCurrentPortableTree(notebook);
    AcceptedHistory before = acceptedHistory(notebook);

    relationController.moveNoteToFolder(force, mechanics);

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(
        after.tipPaths(),
        containsInAnyOrder(
            "physics/Energy.md", "physics/force.png", "mechanics/Force.md", "mechanics/force.png"));
    assertThat(tipText(after, "physics/Energy.md"), equalTo(ENERGY_BODY));
    assertThat(tipText(after, "mechanics/Force.md"), equalTo(FORCE_BODY));
    assertThat(tipContent(after, "physics/force.png"), equalTo(pointer));
    assertThat(tipContent(after, "mechanics/force.png"), equalTo(pointer));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }
}
