package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that retrying an already accepted deletion publication is idempotent: the accepted head,
 * binding timestamp and accepted history stay as the first publication left them, and the deleted
 * note's complete dependent closure stays absent.
 */
class NotebookGitDeletionPublicationRetryControllerTest extends NotebookGitControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void retriesAnAcceptedDeletionWithoutChangingHeadOrResurrectingTheNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(ORIGINAL_CONTENT).please();
    Note retained =
        makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    // Complete dependent fixture on the Target note: memory_tracker + recall_prompt + mcq +
    // conversation, so the retry can prove the whole closure stays absent (no second
    // deletion runs, nothing resurrects).
    MemoryTracker targetTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(target.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(
                  memoryTrackerRepository.findById(targetTracker.getId()).orElseThrow())
              .withMcqForNote(noteRepository.findById(target.getId()).orElseThrow())
              .please();
          makeMe
              .aConversation()
              .forANote(noteRepository.findById(target.getId()).orElseThrow())
              .please();
        });
    NotebookGitBinding initialBinding = snapshotCurrentPortableTree(notebook);
    String initialHead = initialBinding.getAcceptedGitObjectId();
    byte[] proposalBytes =
        proposalBundleBytes(
            initialBinding, List.of(new NotebookGitProposalFile("Retained.md", ORIGINAL_CONTENT)));

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);
    PublicationState stateAfterPublication = publicationState(notebook, target);

    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
    String retriedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    PublicationState stateAfterRetry = publicationState(notebook, target);
    assertThat(stateAfterRetry.acceptedHead(), equalTo(stateAfterPublication.acceptedHead()));
    assertThat(
        stateAfterRetry.bindingUpdatedAt(), equalTo(stateAfterPublication.bindingUpdatedAt()));
    assertThat(stateAfterRetry.acceptedHistory(), equalTo(stateAfterPublication.acceptedHistory()));
    assertThat(stateAfterRetry.notePresent(), equalTo(stateAfterPublication.notePresent()));
    // The deleted note's complete dependent closure stays absent across the retry: the accepted
    // proposal identity matches, so no second deletion runs and nothing resurrects.
    assertThat(stateAfterRetry.dependentCounts(), equalTo(stateAfterPublication.dependentCounts()));
    assertThat(stateAfterRetry.dependentCounts(), equalTo(DependentCounts.allAbsent()));
    // The retained note and its container are intact.
    Note reloadedRetained = noteRepository.findById(retained.getId()).orElseThrow();
    assertThat(reloadedRetained.getContent(), equalTo(ORIGINAL_CONTENT));
    assertThat(noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(1));
  }

  private PublicationState publicationState(Notebook notebook, Note note) throws Exception {
    AcceptedHistory acceptedHistory = acceptedHistory(notebook);
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          boolean notePresent = noteRepository.findById(note.getId()).isPresent();
          return new PublicationState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              acceptedHistory,
              notePresent,
              dependentCounts(note));
        });
  }

  private record PublicationState(
      String acceptedHead,
      Timestamp bindingUpdatedAt,
      AcceptedHistory acceptedHistory,
      boolean notePresent,
      DependentCounts dependentCounts) {}
}
