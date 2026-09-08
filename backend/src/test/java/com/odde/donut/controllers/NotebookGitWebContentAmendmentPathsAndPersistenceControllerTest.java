package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotebookGitWebContentAmendmentPathsAndPersistenceControllerTest
    extends NotebookGitWebContentAmendmentControllerTestSupport {

  @Test
  void rootAndNestedOrdinaryPathsBatchTheSameWay() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder fieldNotes = makeMe.aFolder().notebook(notebook).name("Field Notes").please();
    Note root = makeMe.aNote().notebook(notebook).title("Root").content(ACCEPTED_CONTENT).please();
    Note nested =
        makeMe.aNote().folder(fieldNotes).title("Nested").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    saveAt(root.getId(), content("root-1"), T1000);
    saveAt(root.getId(), content("root-2"), T1008);
    assertThat(contentChain(notebook.getId(), "Root.md").size(), is(2));

    saveAt(nested.getId(), content("nested-1"), T1016);
    saveAt(nested.getId(), content("nested-2"), Instant.parse("2026-09-08T10:20:00Z"));
    assertThat(contentChain(notebook.getId(), "Field Notes/Nested.md").size(), is(3));
  }

  @Test
  void freshPersistenceContextsRetainCandidateAndFrozenState() throws Exception {
    Fixture fixture = fixture("Fresh");
    saveAt(fixture.noteId(), content("first"), T1000);
    NotebookGitBinding eligible =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    assertThat(eligible.getAmendmentNoteId(), equalTo(fixture.noteId()));

    saveAt(fixture.noteId(), content("second"), T1008);
    assertThat(contentChain(fixture.notebookId(), "Fresh.md").size(), is(2));

    controller.downloadNotebookGitBundle(
        notebookRepository.findById(fixture.notebookId()).orElseThrow());
    NotebookGitBinding frozen =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    assertThat(frozen.getAmendmentHead(), nullValue());
  }

  @Test
  void rejectedSaveLeavesNoteHeadAndCandidateUnchanged() throws Exception {
    Fixture fixture = fixture("Reject");
    saveAt(fixture.noteId(), content("eligible"), T1000);
    NotebookGitBinding before =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    String head = before.getAcceptedGitObjectId();
    byte[] bundle = before.getBundleBytes();
    Integer noteId = before.getAmendmentNoteId();
    Timestamp lastChanged = before.getAmendmentLastChangedAt();
    User owner = currentUser.getUser();

    currentUser.setUser(createFixtureUser());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            textContentController.updateNoteContent(
                noteRepository.findById(fixture.noteId()).orElseThrow(),
                contentDto(content("denied"))));
    currentUser.setUser(owner);
    assertThrows(
        ApiException.class,
        () ->
            textContentController.updateNoteContent(
                noteRepository.findById(fixture.noteId()).orElseThrow(),
                contentDto("---\ntype: Note\naliases: invalid\n---\nbody")));

    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(head));
    assertThat(after.getBundleBytes(), equalTo(bundle));
    assertThat(after.getAmendmentNoteId(), equalTo(noteId));
    assertThat(after.getAmendmentLastChangedAt(), equalTo(lastChanged));
    assertThat(
        noteRepository.findById(fixture.noteId()).orElseThrow().getContent(),
        is(content("eligible")));
  }
}
