package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
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
 * Verifies relocation constructs missing destination ancestry for a synchronized source, still
 * refuses pre-existing projection drift, and accepts a represented ancestor with no own README.
 * Placement identity is covered in {@link NotebookGitProposalRelocationControllerTest}.
 * Emptied-source dissolution is covered in {@link
 * NotebookGitProposalRelocationContainerControllerTest}. Addition parent eligibility is covered in
 * {@link NotebookGitFolderNotePublicationControllerTest}.
 */
class NotebookGitProposalRelocationDestinationControllerTest extends NotebookGitControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Autowired NoteController noteController;
  @Autowired FolderRepository folderRepository;

  @Test
  void relocatesIntoAMissingFolderCreatingRequiredParents() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Missing/note.md", TYPED_NOTE_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposal);

    NoteRealm shown = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(shown.getNote().getTitle(), equalTo("note"));
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getName).toList(), contains("Missing"));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Folder::getName)
                    .toList(),
                contains("Missing")));
  }

  @Test
  void rejectsRelocationWhenAnEmptyDestinationAppearedOutsideAcceptedHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    Folder physics = makeMe.aFolder().notebook(notebook).name("Physics").please();
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Physics/note.md", TYPED_NOTE_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("differs from accepted main"));
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
  void relocatesIntoAFolderDissolvedByAnAcceptedMoveByRecreatingTheParent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    Note other =
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

    controller.publishNotebookGitProposal(
        notebook.getId(), afterEmptying.getAcceptedGitObjectId(), intoEmptied);

    NoteRealm shown = noteController.showNote(noteRepository.findById(other.getId()).orElseThrow());
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getName).toList(), contains("Source"));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Folder::getName)
                    .toList(),
                contains("Dest", "Source")));
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
