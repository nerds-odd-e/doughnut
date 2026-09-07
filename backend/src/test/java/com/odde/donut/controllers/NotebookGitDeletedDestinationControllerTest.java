package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.ApiException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies that a deleted note continues to reserve its Portable destination. */
class NotebookGitDeletedDestinationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal content.\n";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nProposed content.\n";
  private static final String RESERVED_TITLE = "Reserved destination";
  private static final String RESERVED_PATH = RESERVED_TITLE + ".md";
  private static final String SOFT_DELETED_TITLE_REASON =
      "A note with this title already exists here but was deleted. Restore the deleted note"
          + " (Undo delete), or choose another title.";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void rejectsASamePathAdditionAfterAcceptedDeletionWithoutResurrectingTheNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note retained =
        makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    AcceptedDeletion deletion = acceptDeletionReservingTitle(notebook, retained);

    byte[] additionProposal =
        proposalBundleBytes(
            deletion.afterDeletionBinding(),
            List.of(
                new NotebookGitProposalFile("Retained.md", ORIGINAL_CONTENT),
                new NotebookGitProposalFile(RESERVED_PATH, PROPOSED_CONTENT)));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            deletion.afterDeletion().acceptedHead(),
            additionProposal,
            ApiException.class);

    assertContextualSoftDeletedTitleConflict(exception, deletion.reserved(), RESERVED_PATH);

    PublicationState afterRejection =
        publicationState(notebook, deletion.reserved(), deletion.tracker());
    assertThat(afterRejection.noteDeletedAt(), equalTo(deletion.afterDeletion().noteDeletedAt()));
    assertThat(
        afterRejection.trackerDeletedAt(), equalTo(deletion.afterDeletion().trackerDeletedAt()));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Note::getId)
                    .toList(),
                contains(retained.getId())));
  }

  @Test
  void rejectsASameParentRenameIntoAnAcceptedDeletionWithoutResurrectingOrMutatingTheSource()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note renamedSource =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Renamed source")
            .content(ORIGINAL_CONTENT)
            .please();
    AcceptedDeletion deletion = acceptDeletionReservingTitle(notebook, renamedSource);
    Timestamp sourceUpdatedAtBeforeRename =
        noteRepository.findById(renamedSource.getId()).orElseThrow().getUpdatedAt();

    byte[] renameProposal =
        proposalBundleBytes(
            deletion.afterDeletionBinding(),
            List.of(new NotebookGitProposalFile(RESERVED_PATH, ORIGINAL_CONTENT)));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, deletion.afterDeletion().acceptedHead(), renameProposal, ApiException.class);

    assertContextualSoftDeletedTitleConflict(exception, deletion.reserved(), RESERVED_PATH);

    PublicationState afterRejection =
        publicationState(notebook, deletion.reserved(), deletion.tracker());
    assertThat(afterRejection.noteDeletedAt(), equalTo(deletion.afterDeletion().noteDeletedAt()));
    assertThat(
        afterRejection.trackerDeletedAt(), equalTo(deletion.afterDeletion().trackerDeletedAt()));
    Note reloadedSource = noteRepository.findById(renamedSource.getId()).orElseThrow();
    assertThat(reloadedSource.getTitle(), equalTo("Renamed source"));
    assertThat(reloadedSource.getUpdatedAt(), equalTo(sourceUpdatedAtBeforeRename));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Note::getId)
                    .toList(),
                contains(renamedSource.getId())));
  }

  @Test
  void rejectsARelocationIntoAnAcceptedDeletionWithoutResurrectingOrMutatingTheSource()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    Note relocating =
        makeMe.aNote().folder(source).title("note").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().folder(destination).title("other").content(ORIGINAL_CONTENT).please();
    AcceptedDeletion deletion =
        acceptDeletionReservingTitle(
            notebook,
            destination,
            List.of(
                new NotebookGitProposalFile("Source/note.md", ORIGINAL_CONTENT),
                new NotebookGitProposalFile("Dest/other.md", ORIGINAL_CONTENT)));
    String reservedPath = "Dest/" + RESERVED_PATH;

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            deletion.afterDeletion().acceptedHead(),
            proposalBundleBytes(
                deletion.afterDeletionBinding(),
                List.of(
                    new NotebookGitProposalFile("Dest/other.md", ORIGINAL_CONTENT),
                    new NotebookGitProposalFile(reservedPath, ORIGINAL_CONTENT))),
            ApiException.class);

    assertContextualSoftDeletedTitleConflict(exception, deletion.reserved(), reservedPath);

    PublicationState afterRejection =
        publicationState(notebook, deletion.reserved(), deletion.tracker());
    assertThat(afterRejection.noteDeletedAt(), equalTo(deletion.afterDeletion().noteDeletedAt()));
    assertThat(
        afterRejection.trackerDeletedAt(), equalTo(deletion.afterDeletion().trackerDeletedAt()));
    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloadedSource = noteRepository.findById(relocating.getId()).orElseThrow();
          assertThat(reloadedSource.getTitle(), equalTo("note"));
          assertThat(reloadedSource.getFolder().getId(), equalTo(source.getId()));
        });
  }

  private void assertContextualSoftDeletedTitleConflict(
      ApiException exception, Note reserved, String reservedPath) {
    assertThat(exception.getErrorBody().getMessage(), containsString(reservedPath));
    assertThat(exception.getErrorBody().getMessage(), containsString(SOFT_DELETED_TITLE_REASON));
    assertThat(
        exception.getErrorBody().getErrorType(),
        is(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
    assertThat(
        exception.getErrorBody().getErrors().get("deletedNoteId"),
        equalTo(String.valueOf(reserved.getId())));
    assertThat(
        exception.getErrorBody().getErrors().get("_originalMessage"),
        equalTo(SOFT_DELETED_TITLE_REASON));
    ApiException original = assertInstanceOf(ApiException.class, exception.getCause());
    assertThat(original.getMessage(), equalTo(SOFT_DELETED_TITLE_REASON));
    assertThat(exception.getErrorBody().getErrors(), equalTo(original.getErrorBody().getErrors()));
  }

  /**
   * Soft-deletes a tracked "Reserved destination" note via an accepted isolated deletion, keeping
   * {@code remainingFiles} in the accepted tree, so the title stays reserved in {@code
   * reservedFolder} (notebook root when null).
   */
  private AcceptedDeletion acceptDeletionReservingTitle(Notebook notebook, Note survivingNote)
      throws Exception {
    return acceptDeletionReservingTitle(
        notebook,
        null,
        List.of(new NotebookGitProposalFile(survivingNote.getTitle() + ".md", ORIGINAL_CONTENT)));
  }

  private AcceptedDeletion acceptDeletionReservingTitle(
      Notebook notebook, Folder reservedFolder, List<NotebookGitProposalFile> remainingFiles)
      throws Exception {
    Note reserved =
        (reservedFolder == null
                ? makeMe.aNote().notebook(notebook)
                : makeMe.aNote().folder(reservedFolder))
            .title(RESERVED_TITLE)
            .content(ORIGINAL_CONTENT)
            .please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(reserved.getId()).orElseThrow())
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] deletionProposal = proposalBundleBytes(binding, remainingFiles);

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), deletionProposal);

    PublicationState afterDeletion = publicationState(notebook, reserved, tracker);
    NotebookGitBinding afterDeletionBinding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    return new AcceptedDeletion(reserved, tracker, afterDeletion, afterDeletionBinding);
  }

  private record AcceptedDeletion(
      Note reserved,
      MemoryTracker tracker,
      PublicationState afterDeletion,
      NotebookGitBinding afterDeletionBinding) {}

  private PublicationState publicationState(Notebook notebook, Note note, MemoryTracker tracker) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
          MemoryTracker reloadedTracker =
              memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          return new PublicationState(
              binding.getAcceptedGitObjectId(),
              reloadedNote.getDeletedAt(),
              reloadedTracker.getDeletedAt());
        });
  }

  private record PublicationState(
      String acceptedHead, Timestamp noteDeletedAt, Timestamp trackerDeletedAt) {}
}
