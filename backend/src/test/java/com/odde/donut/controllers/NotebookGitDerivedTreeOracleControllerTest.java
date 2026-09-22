package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.NotebookGitTreeContent;
import com.odde.donut.services.notebookTree.NotebookLivePortableTree;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every web change's accepted tree, derived from the change, must equal the tree a full assembly of
 * the notebook's projection would produce.
 */
class NotebookGitDerivedTreeOracleControllerTest extends NotebookGitWebContentControllerTestBase {

  @Autowired NotebookLivePortableTree livePortableTree;

  @Test
  void contentEditMatchesTheFullAssembly() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).content(ACCEPTED_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Unrelated").please();
    storeFolderAttachmentAndSnapshot(notebook, null, "picture.bin", new byte[64]);

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  private void assertAcceptedTreeMatchesTheFullAssembly(Notebook notebook) throws Exception {
    assertThat(
        NotebookGitTreeContent.of(acceptedHistory(notebook).tipContent()).blobIds(),
        equalTo(NotebookGitTreeContent.of(livePortableTree.entriesOf(notebook)).blobIds()));
  }
}
