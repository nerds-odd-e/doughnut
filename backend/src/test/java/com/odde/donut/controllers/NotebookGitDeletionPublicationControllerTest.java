package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies isolated learned-note deletion publication across Note projection and Git history. */
class NotebookGitDeletionPublicationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";

  @Autowired NoteController noteController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesAnIsolatedLearnedNoteDeletionAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(ORIGINAL_CONTENT).please();
    Note retained =
        makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    MemoryTracker targetTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(target.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    MemoryTracker retainedTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(retained.getId()).orElseThrow())
                    .difficulty(5f)
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Retained.md", ORIGINAL_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(acceptedHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(liveNotes, hasSize(1));
    assertThat(liveNotes.getFirst().getId(), equalTo(retained.getId()));
    Note reloadedTarget = noteRepository.findById(target.getId()).orElseThrow();
    assertThat(reloadedTarget.getDeletedAt(), notNullValue());
    MemoryTracker deletedTracker =
        memoryTrackerRepository.findById(targetTracker.getId()).orElseThrow();
    assertThat(deletedTracker.getDeletedAt(), notNullValue());
    assertThat(deletedTracker.getDifficulty(), equalTo(targetTracker.getDifficulty()));
    assertThat(deletedTracker.getStability(), equalTo(targetTracker.getStability()));
    MemoryTracker stillLiveTracker =
        memoryTrackerRepository.findById(retainedTracker.getId()).orElseThrow();
    assertThat(stillLiveTracker.getDeletedAt(), nullValue());
    assertThat(noteController.getNoteInfo(reloadedTarget).getMemoryTrackers(), hasSize(0));
    NoteRecallInfo retainedRecall =
        noteController.getNoteInfo(noteRepository.findById(retained.getId()).orElseThrow());
    assertThat(retainedRecall.getMemoryTrackers(), hasSize(1));
    assertThat(
        retainedRecall.getMemoryTrackers().getFirst().getId(), equalTo(retainedTracker.getId()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
    }
  }
}
