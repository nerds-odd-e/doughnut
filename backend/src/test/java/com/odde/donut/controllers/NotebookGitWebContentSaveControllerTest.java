package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitWebContentSaveControllerTest extends NotebookGitWebContentControllerTestBase {

  @Test
  void canonicalNoOpSaveKeepsAcceptedHistoryWhileUpdatingTheNoteTimestamp() throws Exception {
    Timestamp editedAt = Timestamp.from(Instant.parse("2026-09-06T04:05:06Z"));
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    String acceptedHead = accepted.getAcceptedGitObjectId();
    var acceptedHistoryBefore = acceptedHistory(notebook);
    testabilitySettings.timeTravelTo(editedAt);

    textContentController.updateNoteContent(
        note, contentDto("---\ntype: note\n---\naccepted content"));

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    NotebookGitBinding after = binding(notebook);
    assertThat(reloaded.getContent(), is(ACCEPTED_CONTENT));
    assertThat(reloaded.getUpdatedAt(), is(editedAt));
    assertThat(after.getAcceptedGitObjectId(), is(acceptedHead));
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
  }

  @Test
  void repeatedCanonicalNoOpSaveKeepsTheRevisionCreatedByTheChangedSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));
    NotebookGitBinding changed = binding(notebook);
    String changedHead = changed.getAcceptedGitObjectId();
    Timestamp repeatedAt = Timestamp.from(Instant.parse("2026-09-06T05:06:07Z"));
    testabilitySettings.timeTravelTo(repeatedAt);

    textContentController.updateNoteContent(
        note, contentDto("---\ntype: note\n---\nedited content"));

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    NotebookGitBinding after = binding(notebook);
    assertThat(reloaded.getContent(), is(EDITED_CONTENT));
    assertThat(reloaded.getUpdatedAt(), is(repeatedAt));
    // The repeated save re-canonicalizes to the same content the changed save already accepted, so
    // the accepted head - the durable identity of "the revision created by the changed save" - must
    // stay exactly what it was; no second commit is appended.
    assertThat(after.getAcceptedGitObjectId(), is(changedHead));
  }

  @Test
  void savedContentIsStoredAndCommittedWithLfLineEndings() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    String lfContent = "---\ntype: Note\n---\nline one\nline two";

    textContentController.updateNoteContent(
        note, contentDto("---\r\ntype: Note\r\n---\r\nline one\r\nline two"));

    assertThat(noteRepository.findById(note.getId()).orElseThrow().getContent(), is(lfContent));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, head, "Root Note.md"), is(lfContent));
    }
  }

  @Test
  void preExistingPortableDriftIsNeitherBlockingTheWebSaveNorAdoptedByIt() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    var acceptedHistoryBefore = acceptedHistory(notebook);
    makeMe.aNote().notebook(notebook).title("Unsynchronized").content(ACCEPTED_CONTENT).please();

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedHistoryBefore.commits()));
    assertThat(after.tipPaths(), contains("Root Note.md"));
  }

  @Test
  void missingBindingKeepsTheExistingWebSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    notebookGitBindingRepository.delete(binding(notebook));

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), is(EDITED_CONTENT));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isEmpty(), is(true));
  }

  @Test
  void deniedAndInvalidSavesDoNotAdvanceAcceptedHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    var acceptedHistoryBefore = acceptedHistory(notebook);
    User owner = currentUser.getUser();

    currentUser.setUser(createFixtureUser());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));
    currentUser.setUser(owner);
    assertThrows(
        ApiException.class,
        () ->
            textContentController.updateNoteContent(
                note, contentDto("---\ntype: Note\naliases: invalid\n---\nbody")));

    NotebookGitBinding after = binding(notebook);
    assertThat(after.getAcceptedGitObjectId(), is(accepted.getAcceptedGitObjectId()));
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), is(ACCEPTED_CONTENT));
  }

  @Test
  void unreadableAcceptedHistoryFailsLoudlyWithoutSavingTheNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    String acceptedHead = accepted.getAcceptedGitObjectId();
    deleteNativeObjectStoreRow(accepted.getId(), acceptedHead);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));

    assertThat(failure instanceof ResponseStatusException, is(false));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), is(ACCEPTED_CONTENT));
    assertThat(binding(notebook).getAcceptedGitObjectId(), is(acceptedHead));
  }
}
