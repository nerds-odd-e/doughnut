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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that a later, separately authored content-edit commit published against a rename's
 * accepted head updates the same learned concept: the rename is not undone or batched into the
 * edit, and the edit does not disturb the identity or tracker the rename already preserved.
 */
class NotebookGitRenameThenEditControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nORIGINAL_CONTENT";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nEDITED_CONTENT";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesALaterSeparatelyAuthoredEditAtTheRenamedPathOntoTheSameNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("Original").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(original.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] renameProposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Renamed.md", ORIGINAL_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), renameProposal);

    NotebookGitBinding afterRename =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    byte[] editProposal =
        proposalBundleBytes(
            afterRename, List.of(new NotebookGitProposalFile("Renamed.md", EDITED_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), afterRename.getAcceptedGitObjectId(), editProposal);

    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(liveNotes, hasSize(1));
    Note edited = liveNotes.getFirst();
    assertThat(edited.getId(), equalTo(original.getId()));
    assertThat(edited.getTitle(), equalTo("Renamed"));
    assertThat(edited.getContent(), equalTo(EDITED_CONTENT));

    MemoryTracker reloadedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(reloadedTracker.getNote().getId(), equalTo(original.getId()));
    assertThat(reloadedTracker.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
  }
}
