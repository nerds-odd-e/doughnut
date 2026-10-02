package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

class NotebookGitWebContentLearningPreservationControllerTest
    extends NotebookGitWebContentControllerTestBase {

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
            commit.getAuthorIdent().getName(), is(NotebookGitCommitBuilder.SYSTEM_AUTHOR_NAME));
        assertThat(
            commit.getAuthorIdent().getEmailAddress(),
            is(NotebookGitCommitBuilder.SYSTEM_AUTHOR_EMAIL));
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
  void editingTheBodyOfAListNotePreservesLearnedValueTrackersAndAcceptedAncestry()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String original = "---\ntype: Note\ntopic: [A, B]\n---\nOriginal body";
    String edited = original.replace("Original body", "Edited body");
    Note note = makeMe.aNote().notebook(notebook).title("List Note").content(original).please();
    var trackerIds = learnedListTrackers(note);
    var learningBefore = trackerIds.stream().map(this::learning).toList();
    snapshotCurrentPortableTree(notebook);
    var historyBefore = acceptedHistory(notebook);

    textContentController.updateNoteContent(note, contentDto(edited));

    Note stored = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(noteController.showNote(stored).getNote().getContent(), is(edited));
    assertThat(
        noteController.getNoteInfo(stored).getMemoryTrackers().stream()
            .map(MemoryTracker::getId)
            .sorted()
            .toList(),
        equalTo(trackerIds.stream().sorted().toList()));
    assertThat(trackerIds.stream().map(this::learning).toList(), equalTo(learningBefore));
    var historyAfter = acceptedHistory(notebook);
    assertThat(historyAfter.parents(), equalTo(historyBefore.commits()));
    assertThat(tipText(historyAfter, "List Note.md"), is(edited));
  }
}
