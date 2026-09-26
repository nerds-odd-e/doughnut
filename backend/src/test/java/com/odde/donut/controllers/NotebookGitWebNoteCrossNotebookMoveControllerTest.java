package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A note moved to another notebook lands in one accepted commit in each notebook. */
class NotebookGitWebNoteCrossNotebookMoveControllerTest extends NotebookGitWebNoteMoveTestBase {
  Notebook science;
  Notebook engineering;
  Folder physics;
  Folder mechanics;
  Note force;
  AcceptedHistory scienceBefore;
  AcceptedHistory engineeringBefore;

  @BeforeEach
  void seedForceInScienceAndMechanicsInEngineering() throws Exception {
    science = createGitBackedNotebook("Science");
    physics = makeMe.aFolder().notebook(science).name("physics").please();
    force = makeMe.aNote("Force").folder(physics).please();
    Note energy = makeMe.aNote("Energy").notebook(science).please();
    authorReferencingContentCommitted(force, "---\nimage: force.png\n---\nSee [[Energy]].");
    authorReferencingContentCommitted(energy, "See [[Force]].");
    storeFolderAttachmentAndSnapshot(science, physics, "force.png", new byte[] {7});
    engineering = createGitBackedNotebook("Engineering");
    mechanics = makeMe.aFolder().notebook(engineering).name("mechanics").please();
    snapshotCurrentPortableTree(engineering);
    scienceBefore = acceptedHistory(science);
    engineeringBefore = acceptedHistory(engineering);
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));
  }

  @Test
  void movingIntoAFolderOfAnotherNotebookCommitsOnceInEach() throws Exception {
    relationController.moveNoteToFolder(force, mechanics);

    AcceptedHistory scienceAfter = acceptedHistory(science);
    AcceptedHistory engineeringAfter = acceptedHistory(engineering);
    assertThat(scienceAfter.parents(), equalTo(scienceBefore.commits()));
    assertThat(engineeringAfter.parents(), equalTo(engineeringBefore.commits()));
    assertThat(scienceAfter.tipPaths(), containsInAnyOrder("Energy.md", "physics/force.png"));
    assertThat(engineeringAfter.tipPaths(), containsInAnyOrder("mechanics/Force.md"));
    assertThat(tipText(scienceAfter, "Energy.md"), equalTo("See [[Engineering:Force|Force]]."));
    assertThat(
        tipText(engineeringAfter, "mechanics/Force.md"),
        equalTo("---\nimage: force.png\n---\nSee [[Science:Energy|Energy]]."));
    assertThat(reloadNote(force).getNotebook().getId(), equalTo(engineering.getId()));
    assertAcceptedTreeMatchesTheFullAssembly(science);
    assertAcceptedTreeMatchesTheFullAssembly(engineering);
  }

  @Test
  void movingToTheRootOfAnotherNotebookCommitsOnceInEach() throws Exception {
    relationController.moveNoteToNotebookRootInNotebook(force, engineering);

    AcceptedHistory scienceAfter = acceptedHistory(science);
    AcceptedHistory engineeringAfter = acceptedHistory(engineering);
    assertThat(scienceAfter.parents(), equalTo(scienceBefore.commits()));
    assertThat(engineeringAfter.parents(), equalTo(engineeringBefore.commits()));
    assertThat(scienceAfter.tipPaths(), containsInAnyOrder("Energy.md", "physics/force.png"));
    assertThat(engineeringAfter.tipPaths(), containsInAnyOrder("mechanics/.keep", "Force.md"));
    assertAcceptedTreeMatchesTheFullAssembly(science);
    assertAcceptedTreeMatchesTheFullAssembly(engineering);
  }

  @Test
  void movingBackAsUndoRestoresBothNotebooks() throws Exception {
    relationController.moveNoteToFolder(force, mechanics);

    relationController.moveNoteToFolder(reloadNote(force), physics);

    assertThat(
        acceptedHistory(science).tipPaths(),
        containsInAnyOrder("Energy.md", "physics/Force.md", "physics/force.png"));
    assertThat(acceptedHistory(engineering).tipPaths(), containsInAnyOrder("mechanics/.keep"));
    assertAcceptedTreeMatchesTheFullAssembly(science);
    assertAcceptedTreeMatchesTheFullAssembly(engineering);
  }
}
