package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Verifies note creation inside folders represented by accepted Portable content. */
class NotebookGitFolderNotePublicationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String EXISTING_CONTENT = "---\ntype: Note\n---\nExisting content.\n";
  private static final String CREATED_CONTENT = "---\ntype: Note\n---\nAuthored inertia content.\n";
  private static final String SECOND_CONTENT = "---\ntype: Note\n---\nAuthored momentum content.\n";

  @Autowired NoteController noteController;
  @Autowired FolderRepository folderRepository;

  @Test
  void publishesSeveralNotesInsideTheExistingPortableFolderAsTheExactAuthoredCommit()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("Physics").please();
    makeMe.aNote().folder(physics).title("Motion").content(EXISTING_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Physics/Motion.md", EXISTING_CONTENT),
                new NotebookGitProposalFile("Physics/Inertia.md", CREATED_CONTENT),
                new NotebookGitProposalFile("Physics/Momentum.md", SECOND_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposed = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposed, proposal);
    }
    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposal);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(3));
    Map<String, String> additions = Map.of("Inertia", CREATED_CONTENT, "Momentum", SECOND_CONTENT);
    for (var addition : additions.entrySet()) {
      Note created =
          notes.stream()
              .filter(note -> note.getTitle().equals(addition.getKey()))
              .findFirst()
              .orElseThrow();
      NoteRealm shown = noteController.showNote(created);
      assertThat(
          shown.getAncestorFolders().stream().map(Folder::getId).toList(),
          contains(physics.getId()));
      assertThat(shown.getNote().getContent(), equalTo(addition.getValue()));
    }
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(1));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    byte[] downloaded = controller.downloadNotebookGitBundle(acceptedNotebook).getBody();
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded);
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
      for (var addition : additions.entrySet()) {
        assertThat(
            NotebookGitProposalBlobText.readUtf8(
                readBack, downloadedCommit.head(), "Physics/" + addition.getKey() + ".md"),
            equalTo(addition.getValue()));
      }
    }
  }

  @Test
  void selectsANestedReadmeOnlyFolderByItsFullPortablePath() throws Exception {
    Notebook notebook = createGitBackedNotebook();
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
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "Physics/README.md", "---\ntype: Readme\n---\nRoot physics readme"),
                new NotebookGitProposalFile(
                    "Courses/Physics/README.md", "---\ntype: Readme\n---\nCourse physics readme"),
                new NotebookGitProposalFile("Courses/Physics/Inertia.md", CREATED_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposal);

    Note created =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).getFirst();
    assertThat(created.getFolder().getId(), equalTo(nestedPhysics.getId()));
  }

  @Test
  void rejectsAParentFolderThatIsNotRepresentedInAcceptedPortableContent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aFolder().notebook(notebook).name("Physics").please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Physics/Inertia.md", CREATED_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Physics/Inertia.md"));
    assertThat(
        exception.getReason(), containsString("not represented in accepted Portable content"));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), empty());
  }
}
