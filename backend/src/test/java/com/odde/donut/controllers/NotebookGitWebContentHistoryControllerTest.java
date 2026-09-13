package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

class NotebookGitWebContentHistoryControllerTest
    extends NotebookGitWebContentHistoryControllerTestSupport {

  private static final String NOTE_PATH = "Root Note.md";
  private static final String CONTENT_1000 = "---\ntype: Note\n---\ncontent at 10:00";
  private static final String CONTENT_1008 = "---\ntype: Note\n---\ncontent at 10:08";

  @Test
  void rapidSameNoteSavesAppendEachRevisionWithContent() throws Exception {
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
    ObjectId preEditParent = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    saveAt(note.getId(), CONTENT_1000, T1000);
    ObjectId firstEdit = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    saveAt(note.getId(), CONTENT_1008, T1008);
    ObjectId secondEdit = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(reloaded.getContent(), is(CONTENT_1008));
    assertThat(reloaded.getUpdatedAt(), is(Timestamp.from(T1008)));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getId(),
        is(tracker.getId()));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getDifficulty(),
        is(7f));

    History history = downloadHistory(notebook, NOTE_PATH);
    assertThat(
        history.contentsNewestFirst(),
        equalTo(List.of(CONTENT_1008, CONTENT_1000, ACCEPTED_CONTENT)));
    assertThat(history.headsNewestFirst(), equalTo(List.of(secondEdit, firstEdit, preEditParent)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId tip = GitBundleTestReader.fetchHead(repository, history.bundleBytes());
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(tip);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), equalTo(firstEdit));
        assertThat(
            commit.getAuthorIdent().getName(), is(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
        assertThat(
            commit.getAuthorIdent().getEmailAddress(),
            is(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
        assertThat(commit.getAuthorIdent().getWhenAsInstant(), equalTo(T1008));
        assertThat(commit.getCommitterIdent(), equalTo(commit.getAuthorIdent()));
        assertThat(commit.getFullMessage(), is("Edit note content: Root Note"));
      }
    }
  }
}
