package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Deleting a file on the web is one accepted change whose tree lacks the file. */
class NotebookGitWebAttachmentDeleteControllerTest extends NotebookGitWebContentControllerTestBase {
  private static final String PLAIN_BODY = "---\ntype: Note\n---\nforce body";
  private static final String FORCE_BODY = "---\ntype: Note\nimage: force.png\n---\nforce body";
  private static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4e, 0x47};
  private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46};

  @Autowired NotebookAttachmentController attachmentController;

  @Test
  void deletingAFileAppendsOneAcceptedChildWithoutIt() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    makeMe.aNote("Force").folder(physics).content(PLAIN_BODY).please();
    storeFolderAttachmentAndSnapshot(notebook, physics, "notes.pdf", PDF_BYTES);
    NotebookAttachment sketch =
        storeFolderAttachmentAndSnapshot(notebook, physics, "sketch.png", PNG_BYTES);
    AcceptedHistory before = acceptedHistory(notebook);

    attachmentController.deleteAttachment(notebook, sketch);

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(after.tipPaths(), not(hasItem("physics/sketch.png")));
    assertThat(
        after.content(),
        hasItems(
            before.content().stream()
                .filter(entry -> !entry.path().equals("physics/sketch.png"))
                .toArray(PortableTreeEntry[]::new)));
    assertThat(notebookAttachmentRepository.findById(sketch.getId()).isPresent(), is(false));
  }

  @Test
  void aFileANoteShowsCanBeDeletedAndTheNoteKeepsItsImage() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note force = makeMe.aNote("Force").folder(physics).content(FORCE_BODY).please();
    NotebookAttachment picture =
        storeFolderAttachmentAndSnapshot(notebook, physics, "force.png", PNG_BYTES);

    attachmentController.deleteAttachment(notebook, picture);

    assertThat(
        noteRepository.findById(force.getId()).orElseThrow().getContent(), equalTo(FORCE_BODY));
    assertThat(tipText(acceptedHistory(notebook), "physics/Force.md"), equalTo(FORCE_BODY));
  }

  @Test
  void someoneWhoCannotEditTheNotebookCannotDeleteItsFiles() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookAttachment sketch =
        storeFolderAttachmentAndSnapshot(notebook, null, "sketch.png", PNG_BYTES);
    AcceptedHistory before = acceptedHistory(notebook);
    User owner = currentUser.getUser();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> attachmentController.deleteAttachment(notebook, sketch));

    currentUser.setUser(owner);
    assertThat(acceptedHistory(notebook).commits(), equalTo(before.commits()));
    assertThat(notebookAttachmentRepository.findById(sketch.getId()).isPresent(), is(true));
  }
}
