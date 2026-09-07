package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies that a Git proposal cannot overwrite accepted content or unsupported structural drift.
 */
class NotebookGitProjectionDriftControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nproposed content";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb content";

  @Autowired TextContentController textContentController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void rejectsAnAdditionBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain() throws Exception {
    rejectBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain(this::additionProposalBundle);
  }

  @Test
  void rejectsADeletionBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain() throws Exception {
    Note remaining =
        rejectBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain(
            this::isolatedDeletionProposalBundle);
    assertThat(remaining.getDeletedAt(), nullValue());
  }

  @Test
  void rejectsAnAdditionWhenAWebCreationOccupiesItsDestination() throws Exception {
    rejectWhenAWebCreationHasDriftedTheProjection(this::additionProposalBundle);
  }

  @Test
  void rejectsADeletionWhenAWebCreationHasDriftedTheProjection() throws Exception {
    DriftedWebCreation remaining =
        rejectWhenAWebCreationHasDriftedTheProjection(this::isolatedDeletionProposalBundle);
    assertThat(remaining.acceptedNote().getDeletedAt(), nullValue());
    assertThat(
        memoryTrackerRepository.findById(remaining.tracker().getId()).orElseThrow().getDeletedAt(),
        nullValue());
  }

  private DriftedWebCreation rejectWhenAWebCreationHasDriftedTheProjection(
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
    NoteCreationDTO webCreation = new NoteCreationDTO();
    webCreation.setNewTitle("addition");
    webCreation.setContent("web content");
    Note occupiedDestination =
        noteRepository
            .findById(controller.createNoteAtNotebookRoot(notebook, webCreation).getId())
            .orElseThrow();

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
    assertThat(reloadedOccupiedDestination.getContent(), equalTo(WEB_CONTENT));
    assertThat(reloadedOccupiedDestination.getFolder(), nullValue());
    assertThat(
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(Note::getId)
            .toList(),
        equalTo(List.of(acceptedNote.getId(), occupiedDestination.getId())));
    return new DriftedWebCreation(reloadedAccepted, tracker);
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

    assertThat(exception.getReason(), containsString("expectedHead no longer matches"));
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

  private NotebookGitBinding reloadCommittedBinding(Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () -> notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow());
  }

  @FunctionalInterface
  private interface ProposalBundleFactory {
    byte[] create(NotebookGitBinding binding) throws Exception;
  }

  private record DriftedWebCreation(Note acceptedNote, MemoryTracker tracker) {}
}
