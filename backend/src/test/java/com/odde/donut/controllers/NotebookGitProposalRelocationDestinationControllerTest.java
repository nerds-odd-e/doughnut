package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies relocation rejects destinations that are missing or unrepresented in accepted Portable
 * content, and still accepts a represented ancestor with no own README. Placement identity is
 * covered in {@link NotebookGitProposalRelocationControllerTest}. Emptied-source retention is
 * covered in {@link NotebookGitProposalRelocationContainerControllerTest}. Addition parent
 * eligibility is covered in {@link NotebookGitFolderNotePublicationControllerTest}.
 */
class NotebookGitProposalRelocationDestinationControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Autowired NoteController noteController;
  @Autowired FolderRepository folderRepository;

  @Test
  void rejectsRelocationIntoAMissingFolderWithoutCreatingIt() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Missing/note.md", TYPED_NOTE_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(
        exception.getReason(),
        equalTo(
            "Parent folder for path \"Missing/note.md\" is not represented in accepted Portable"
                + " content; add this note at the notebook root or inside an existing represented"
                + " folder."));
    NoteRealm shown = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(shown.getAncestorFolders(), empty());
    assertThat(shown.getNote().getTitle(), equalTo("note"));
    inCommittedTransaction(
        transactionManager,
        () -> assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), empty()));
  }

  @Test
  void rejectsRelocationIntoAnExistingUnrepresentedFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("Physics").please();
    makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Physics/note.md", TYPED_NOTE_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Physics/note.md"));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Folder::getId)
                    .toList(),
                contains(physics.getId())));
  }

  @Test
  void rejectsRelocationIntoAFolderEmptiedByAnAcceptedMove() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(destination).title("other").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().folder(source).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Dest/other.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/note.md", TYPED_NOTE_CONTENT))));

    NotebookGitBinding afterEmptying =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    byte[] intoEmptied =
        proposalBundleBytes(
            afterEmptying,
            List.of(
                new NotebookGitProposalFile("Dest/note.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Source/other.md", TYPED_NOTE_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, afterEmptying.getAcceptedGitObjectId(), intoEmptied, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Source/other.md"));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Folder::getId)
                    .toList(),
                containsInAnyOrder(source.getId(), destination.getId())));
  }

  @Test
  void relocatesIntoAFolderRepresentedOnlyByDescendantTrackedContent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder courses = makeMe.aFolder().notebook(notebook).name("Courses").please();
    Folder physics = makeMe.aFolder().parentFolder(courses).name("Physics").please();
    makeMe.aNote().folder(physics).title("Motion").content(TYPED_NOTE_CONTENT).please();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Courses/Physics/Motion.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Courses/note.md", TYPED_NOTE_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposal);

    NoteRealm shown = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getId).toList(), contains(courses.getId()));
  }
}
