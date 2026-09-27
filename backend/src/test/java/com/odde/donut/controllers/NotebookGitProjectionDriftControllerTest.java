package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies that a Git proposal cannot overwrite accepted content or unsupported structural drift.
 */
class NotebookGitProjectionDriftControllerTest extends NotebookGitControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nproposed content";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb content";
  private static final String UNSYNCHRONIZED_CONTENT =
      "---\ntype: Note\n---\nunsynchronized content";

  @Autowired TextContentController textContentController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired FolderRepository folderRepository;

  @Test
  void rejectsAnAdditionBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain() throws Exception {
    rejectBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain(this::additionProposalBundle);
  }

  @Test
  void rejectsADeletionBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain() throws Exception {
    rejectBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain(
        this::isolatedDeletionProposalBundle);
  }

  @Test
  void rejectsAnAdditionWhenAnUnsynchronizedNoteOccupiesItsDestination() throws Exception {
    rejectWhenAnUnsynchronizedNoteHasDriftedTheProjection(this::additionProposalBundle);
  }

  @Test
  void rejectsADeletionWhenAnUnsynchronizedNoteHasDriftedTheProjection() throws Exception {
    DriftedProjection remaining =
        rejectWhenAnUnsynchronizedNoteHasDriftedTheProjection(this::isolatedDeletionProposalBundle);
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () ->
                memoryTrackerRepository
                    .findById(remaining.tracker().getId())
                    .orElseThrow()
                    .isActive()),
        equalTo(true));
  }

  @Test
  void rejectsAFolderRelocationWhenAnUnsynchronizedNoteHasDriftedTheProjection() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    makeMe.aNote().notebook(notebook).title("addition").content(UNSYNCHRONIZED_CONTENT).please();

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(
                binding,
                List.of(
                    new NotebookGitProposalFile("Archive/README.md", README),
                    new NotebookGitProposalFile("Archive/Topics/README.md", README),
                    new NotebookGitProposalFile("Archive/Topics/A.md", NOTE))),
            HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("refresh the checkout before publishing"));
    NotebookGitProposalFolderRelocationParentMap.assertUnchanged(
        transactionManager, folderRepository, notebook, archive, topics);
  }

  private DriftedProjection rejectWhenAnUnsynchronizedNoteHasDriftedTheProjection(
      ProposalBundleFactory proposalFactory) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note acceptedNote =
        makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(acceptedNote.getId()).orElseThrow())
                    .please());
    Note occupiedDestination =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("addition")
            .content(UNSYNCHRONIZED_CONTENT)
            .please();

    byte[] proposal = proposalFactory.create(binding);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.CONFLICT);

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(exception.getReason(), containsString("refresh the checkout before publishing"));
    Note reloadedAccepted = noteRepository.findById(acceptedNote.getId()).orElseThrow();
    assertThat(reloadedAccepted.getContent(), equalTo(ACCEPTED_CONTENT));
    Note reloadedOccupiedDestination =
        noteRepository.findById(occupiedDestination.getId()).orElseThrow();
    assertThat(reloadedOccupiedDestination.getTitle(), equalTo("addition"));
    assertThat(reloadedOccupiedDestination.getContent(), equalTo(UNSYNCHRONIZED_CONTENT));
    assertThat(reloadedOccupiedDestination.getFolder(), nullValue());
    assertThat(
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(Note::getId)
            .toList(),
        equalTo(List.of(acceptedNote.getId(), occupiedDestination.getId())));
    return new DriftedProjection(reloadedAccepted, tracker);
  }

  private Note rejectBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain(
      ProposalBundleFactory proposalFactory) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal = proposalFactory.create(binding);
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent(WEB_CONTENT);
    textContentController.updateNoteContent(note, update);
    NotebookGitBinding winningBinding = reloadCommittedBinding(notebook.getId());
    assertThat(
        winningBinding.getAcceptedGitObjectId(), not(equalTo(binding.getAcceptedGitObjectId())));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.CONFLICT);

    assertThat(
        exception.getReason(), containsString("The notebook changed since this publish started"));
    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(reloaded.getContent(), equalTo(update.getContent()));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(),
        equalTo(winningBinding.getAcceptedGitObjectId()));
    return reloaded;
  }

  private byte[] additionProposalBundle(NotebookGitBinding binding) throws Exception {
    return proposalBundleBytes(
        binding,
        List.of(
            new NotebookGitProposalFile("note.md", ACCEPTED_CONTENT),
            new NotebookGitProposalFile("addition.md", PROPOSED_CONTENT)));
  }

  private byte[] isolatedDeletionProposalBundle(NotebookGitBinding binding) throws Exception {
    return proposalBundleBytes(binding, List.of());
  }

  @FunctionalInterface
  private interface ProposalBundleFactory {
    byte[] create(NotebookGitBinding binding) throws Exception;
  }

  private record DriftedProjection(Note acceptedNote, MemoryTracker tracker) {}
}
