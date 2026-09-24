package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

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

/**
 * Verifies deletion followed by recreation at the same path publishes as a new note identity with
 * permanent removal of the old learning closure — including when tip path and bytes equal the
 * accepted tree. Exact unchanged-content moves still retain identity (no false deletion gap).
 */
class NotebookGitDeletionThenRecreationControllerTest extends NotebookGitControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String RECREATED_CONTENT =
      "---\ntype: Note\n---\nRecreated authored bytes.\n";
  private static final String RETAINED_CONTENT = "---\ntype: Note\n---\nRetained authored bytes.\n";
  private static final String RETAINED_EDITED = "---\ntype: Note\n---\nRetained edited bytes.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void recreatesSamePathWithIdenticalBytesAsNewIdentityAndRemovesLearningClosure()
      throws Exception {
    publishDeletionThenRecreation(ORIGINAL_CONTENT);
  }

  @Test
  void recreatesSamePathWithDifferentBytesAsNewIdentityAndRemovesLearningClosure()
      throws Exception {
    publishDeletionThenRecreation(RECREATED_CONTENT);
  }

  @Test
  void exactMoveAcrossRangeStillRetainsIdentity() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("Original").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Retained").content(RETAINED_CONTENT).please();
    MemoryTracker tracker = attachLearningClosure(original);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId afterRename =
          localCommitOnTopOf(
              repository,
              acceptedHead,
              List.of(
                  new NotebookGitProposalFile("Renamed.md", ORIGINAL_CONTENT),
                  new NotebookGitProposalFile("Retained.md", RETAINED_CONTENT)),
              "Exact rename");
      tip =
          localCommitOnTopOf(
              repository,
              afterRename,
              List.of(
                  new NotebookGitProposalFile("Renamed.md", ORIGINAL_CONTENT),
                  new NotebookGitProposalFile("Retained.md", RETAINED_EDITED)),
              "Edit companion after rename");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    Note renamed = noteRepository.findById(original.getId()).orElseThrow();
    assertThat(renamed.getTitle(), equalTo("Renamed"));
    assertThat(renamed.getContent(), equalTo(ORIGINAL_CONTENT));
    assertThat(
        memoryTrackerRepository.findById(tracker.getId()).orElseThrow().isActive(), equalTo(true));
    assertThat(
        inCommittedTransaction(transactionManager, () -> dependentCounts(original)),
        not(equalTo(DependentCounts.allAbsent())));
  }

  private void publishDeletionThenRecreation(String tipContent) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("Target").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Retained").content(RETAINED_CONTENT).please();
    MemoryTracker tracker = attachLearningClosure(original);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId afterDelete =
          localCommitOnTopOf(
              repository,
              acceptedHead,
              List.of(new NotebookGitProposalFile("Retained.md", RETAINED_CONTENT)),
              "Delete target");
      tip =
          localCommitOnTopOf(
              repository,
              afterDelete,
              List.of(
                  new NotebookGitProposalFile("Target.md", tipContent),
                  new NotebookGitProposalFile("Retained.md", RETAINED_CONTENT)),
              "Recreate target at same path");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    assertThat(noteRepository.findById(original.getId()).isPresent(), equalTo(false));
    assertThat(
        inCommittedTransaction(transactionManager, () -> dependentCounts(original)),
        equalTo(DependentCounts.allAbsent()));
    assertThat(memoryTrackerRepository.findById(tracker.getId()).isPresent(), equalTo(false));

    List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(storedNotes, hasSize(2));
    Note recreated =
        storedNotes.stream()
            .filter(note -> "Target".equals(note.getTitle()))
            .findFirst()
            .orElseThrow();
    assertThat(recreated.getId(), not(equalTo(original.getId())));
    assertThat(recreated.getContent(), equalTo(tipContent));
  }

  private MemoryTracker attachLearningClosure(Note note) {
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTrackerRepository.findById(tracker.getId()).orElseThrow())
              .withMcqForNote(noteRepository.findById(note.getId()).orElseThrow())
              .please();
          makeMe.anImage().forNote(noteRepository.findById(note.getId()).orElseThrow()).please();
          makeMe
              .aConversation()
              .forANote(noteRepository.findById(note.getId()).orElseThrow())
              .please();
        });
    return tracker;
  }
}
