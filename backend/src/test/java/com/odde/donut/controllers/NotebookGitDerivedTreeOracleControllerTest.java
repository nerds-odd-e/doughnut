package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every web note change's accepted tree, derived from the change, must equal the tree a full
 * assembly of the notebook's projection would produce.
 */
class NotebookGitDerivedTreeOracleControllerTest extends NotebookGitWebContentControllerTestBase {

  @Autowired RelationController relationController;

  @Test
  void contentEditMatchesTheFullAssembly() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).content(ACCEPTED_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Unrelated").please();
    storeFolderAttachmentAndSnapshot(notebook, null, "picture.bin", new byte[64]);

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void firstNoteInAnEmptyFolderReplacesItsMarkerAndMatchesTheFullAssembly() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder ideas = makeMe.aFolder().notebook(notebook).name("Ideas").please();
    snapshotCurrentPortableTree(notebook);
    assertThat(acceptedHistory(notebook).tipPaths(), contains("Ideas/.keep"));
    NoteCreationDTO creation = new NoteCreationDTO();
    creation.setNewTitle("Plan");
    creation.setFolderId(ideas.getId());

    controller.createNoteAtNotebookRoot(notebook, creation);

    assertThat(acceptedHistory(notebook).tipPaths(), contains("Ideas/Plan.md"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void permanentlyRemovingTheOnlyNoteOfAFolderRestoresItsMarkerAndMatchesTheFullAssembly()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder trashedBiology = makeMe.aFolder().inTrashOf(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(trashedBiology).please();
    snapshotCurrentPortableTree(notebook);

    noteController.permanentlyDeleteNote(cells);

    assertThat(acceptedHistory(notebook).tipPaths(), contains("_trash/Biology/.keep"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void renamingALinkedNoteReplacesItsPathAndItsReferrersOnlyAndMatchesTheFullAssembly()
      throws Throwable {
    Notebook notebook = createGitBackedNotebook();
    Note a = makeMe.aNote("A").notebook(notebook).content(ACCEPTED_CONTENT).please();
    for (String referrer : List.of("First", "Second", "Third")) {
      Note note = makeMe.aNote(referrer).notebook(notebook).please();
      authorReferencingContentCommitted(note, "---\ntype: Note\n---\nSee [[A]].");
    }
    makeMe.aNote("Unrelated").notebook(notebook).please();
    storeFolderAttachmentAndSnapshot(notebook, null, "picture.bin", new byte[64]);
    Map<String, ObjectId> before = acceptedBlobIds(notebook);
    NoteUpdateTitleDTO rename = titleDto("B");
    rename.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    List<String> queries = queriesOf(() -> textContentController.updateNoteTitle(a, rename));

    Map<String, ObjectId> after = acceptedBlobIds(notebook);
    assertThat(after.keySet(), not(hasItem("A.md")));
    assertThat(after.keySet(), hasItem("B.md"));
    List<String> replaced = List.of("A.md", "B.md", "First.md", "Second.md", "Third.md");
    for (String referrer : List.of("First.md", "Second.md", "Third.md")) {
      assertThat(after.get(referrer), not(equalTo(before.get(referrer))));
    }
    assertThat(without(after, replaced), equalTo(without(before, replaced)));
    assertThat(queries, not(hasItem(containsString("NotebookAttachment"))));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void movingANoteToAnotherFolderMovesItsPathAndMarkersAndMatchesTheFullAssembly()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder study = makeMe.aFolder().notebook(notebook).name("Study").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder("Biology/Cells.md", "Study/.keep"));

    relationController.moveNoteToFolder(cells, study);

    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder("Biology/.keep", "Study/Cells.md"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void trashingAndRecoveringTheOnlyNoteOfAFolderMoveItsPathAndMatchTheFullAssembly()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder ideas = makeMe.aFolder().notebook(notebook).name("Ideas").please();
    makeMe.aFolder().inTrashOf(notebook).name("Ideas").please();
    Note plan = makeMe.aNote("Plan").folder(ideas).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    noteController.trashNote(plan, leaveDeadLinks());

    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder("Ideas/.keep", "_trash/Ideas/Plan.md"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);

    noteController.undoTrashNote(
        noteRepository.findById(plan.getId()).orElseThrow(), undoTo("Plan", ideas));

    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder("Ideas/Plan.md", "_trash/Ideas/.keep"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  private static Map<String, ObjectId> without(Map<String, ObjectId> blobIds, List<String> paths) {
    Map<String, ObjectId> rest = new HashMap<>(blobIds);
    paths.forEach(rest::remove);
    return rest;
  }
}
