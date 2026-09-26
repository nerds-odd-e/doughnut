package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
  void savesARootNoteAsOneAcceptedRevisionWithoutChangingItsLearningIdentity() throws Exception {
    Timestamp editedAt = Timestamp.from(Instant.parse("2026-09-06T03:04:05Z"));
    testabilitySettings.timeTravelTo(editedAt);
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    NoteRealm response = textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    NoteRealm readBackNote = noteController.showNote(reloaded);
    assertThat(response.getId(), is(note.getId()));
    assertThat(readBackNote.getId(), is(note.getId()));
    assertThat(readBackNote.getNote().getContent(), is(EDITED_CONTENT));
    assertThat(reloaded.getUpdatedAt(), is(editedAt));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getId(),
        is(tracker.getId()));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getDifficulty(),
        is(7f));

    byte[] downloaded = acceptedBundleBytes(notebook);
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId editedHead = GitBundleTestReader.fetchHead(repository, downloaded);
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(editedHead);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), is(acceptedHead));
        assertThat(
            commit.getAuthorIdent().getName(), is(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
        assertThat(
            commit.getAuthorIdent().getEmailAddress(),
            is(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
        assertThat(commit.getCommitterIdent(), is(commit.getAuthorIdent()));
        assertThat(commit.getFullMessage(), is("Edit note content: Root Note"));
        assertThat(commit.getCommitTime(), is((int) editedAt.toInstant().getEpochSecond()));
        assertThat(revWalk.parseCommit(acceptedHead).getId(), is(acceptedHead));
      }
      assertThat(GitBundleTestReader.pathsIn(repository, editedHead), contains("Root Note.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, editedHead, "Root Note.md"),
          is(EDITED_CONTENT));
      NotebookGitBinding storedBinding = binding(notebook);
      assertThat(storedBinding.getAcceptedGitObjectId(), is(editedHead.getName()));
      assertThat(storedBinding.getUpdatedAt(), is(editedAt));
    }
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
