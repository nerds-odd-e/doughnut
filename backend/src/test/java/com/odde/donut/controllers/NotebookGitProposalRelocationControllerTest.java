package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies {@code publishNotebookGitProposal} relocates an unchanged note to an existing folder or
 * the notebook root, keeping filename and identity. Same-parent filename changes are covered in
 * {@link NotebookGitProposalRenameControllerTest}. Combined parent-and-filename pairs are covered
 * in {@link NotebookGitProposalRelocateAndRenameControllerTest}.
 */
class NotebookGitProposalRelocationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Autowired NoteController noteController;

  @Test
  void relocatesARootNoteIntoAnExistingFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Folder").please();
    makeMe.aNote().folder(folder).title("keep").content(TYPED_NOTE_CONTENT).please();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Folder/keep.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Folder/note.md", TYPED_NOTE_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    NoteRealm shown = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getId).toList(), contains(folder.getId()));
  }

  @Test
  void relocatesAFolderNoteToTheNotebookRoot() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Folder").please();
    makeMe.aNote().folder(folder).title("keep").content(TYPED_NOTE_CONTENT).please();
    Note note = makeMe.aNote().folder(folder).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Folder/keep.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("note.md", TYPED_NOTE_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    NoteRealm shown = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(shown.getAncestorFolders(), empty());
  }

  @Test
  void relocatesANoteBetweenExistingFolders() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(source).title("keep").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().folder(destination).title("other").content(TYPED_NOTE_CONTENT).please();
    Note note = makeMe.aNote().folder(source).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Source/keep.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/other.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/note.md", TYPED_NOTE_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    NoteRealm shown = noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getId).toList(),
        contains(destination.getId()));
  }

  @Test
  void relocatesANoteIntoANestedReadmeOnlyFolderDistinguishedByFullPath() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder rootPhysics =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Physics")
            .readmeContent("Root physics readme")
            .please();
    Folder courses = makeMe.aFolder().notebook(notebook).name("Courses").please();
    Folder nestedPhysics =
        makeMe
            .aFolder()
            .parentFolder(courses)
            .name("Physics")
            .readmeContent("Course physics readme")
            .please();
    Note note =
        makeMe.aNote().folder(rootPhysics).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "Physics/README.md", "---\ntype: Readme\n---\nRoot physics readme"),
                new NotebookGitProposalFile(
                    "Courses/Physics/README.md", "---\ntype: Readme\n---\nCourse physics readme"),
                new NotebookGitProposalFile("Courses/Physics/note.md", TYPED_NOTE_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note relocated = notes.getFirst();
    assertThat(relocated.getId(), equalTo(note.getId()));
    NoteRealm shown = noteController.showNote(relocated);
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getId).toList(),
        contains(courses.getId(), nestedPhysics.getId()));
    assertThat(shown.getNote().getContent(), equalTo(TYPED_NOTE_CONTENT));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent().getName(), equalTo(binding.getAcceptedGitObjectId()));
    }
  }
}
