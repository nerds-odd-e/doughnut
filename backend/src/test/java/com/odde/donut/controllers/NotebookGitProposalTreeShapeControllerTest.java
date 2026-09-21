package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.FileMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies {@code publishNotebookGitProposal}'s tree-shape gating: a proposal that is not an
 * identical-heads no-op must change one or more ordinary Markdown notes (added and/or modified),
 * delete ordinary notes alone or with same-path edits, or rename exactly one ordinary note with
 * unchanged content. Container Readme modifications (root and folder) are admitted alongside
 * ordinary-note changes. Compatible deletion batches are covered in {@link
 * NotebookGitDeletionPublicationControllerTest}. Edits-only acceptance of several existing notes is
 * covered in {@link NotebookGitExistingNoteBatchPublicationControllerTest}. Equal-content rename
 * acceptance is covered in {@link NotebookGitProposalRenameControllerTest}; filename-preserving
 * relocation in {@link NotebookGitProposalRelocationControllerTest}; combined parent-and-filename
 * acceptance in {@link NotebookGitProposalRelocateAndRenameControllerTest}.
 */
class NotebookGitProposalTreeShapeControllerTest extends NotebookGitControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String EDITED_ROOT_README =
      "---\ntype: Readme\nauthor: owner\n---\nreadme changed\n";

  @Test
  void acceptsAChangeToIndexMdJustLikeAnyOtherNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("index").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("index.md", "---\ntype: Note\n---\nchanged content")));

    assertDoesNotThrow(
        () ->
            controller.publishNotebookGitProposal(
                notebook.getId(), binding.getAcceptedGitObjectId(), bundleBytes));
  }

  @Test
  void acceptsAValidRootReadmeEditAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    makeMe.theNotebook(notebook).readmeContent("readme original").please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("README.md", EDITED_ROOT_README)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getId(), equalTo(notebook.getId()));
    assertThat(acceptedNotebook.getReadmeContent(), equalTo(EDITED_ROOT_README));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void acceptsAValidRootReadmeEditAsTheSoleNotebookReadme() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.theNotebook(notebook).readmeContent("readme original").please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("README.md", EDITED_ROOT_README)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), equalTo(EDITED_ROOT_README));
    assertThat(noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()), empty());
  }

  @Test
  void rejectsAnInvalidRootReadmeEditWithACompanionNoteEditWithoutMutatingAcceptedContent()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    makeMe.theNotebook(notebook).readmeContent("readme original").please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", "readme changed, no frontmatter"),
                new NotebookGitProposalFile("note.md", "---\ntype: Note\n---\nchanged content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposalBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("README.md"));
    assertThat(
        notebookRepository.findById(notebook.getId()).orElseThrow().getReadmeContent(),
        equalTo("readme original"));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(),
        equalTo(TYPED_NOTE_CONTENT));
  }

  @Test
  void acceptsAValidFolderReadmeEditAlongsideANoteAdditionWithoutMutatingFolderIdentity()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Folder")
            .readmeContent("original readme")
            .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Added.md", "---\ntype: Note\n---\nadded content"),
                new NotebookGitProposalFile(
                    "Folder/README.md", "---\ntype: Readme\n---\nchanged readme")));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), bundleBytes);

    Folder reloaded = entityManager.find(Folder.class, folder.getId());
    assertThat(reloaded.getReadmeContent(), equalTo("---\ntype: Readme\n---\nchanged readme"));
    List<Note> notes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    assertThat(notes.getFirst().getTitle(), equalTo("Added"));
  }

  @Test
  void rejectsProposalThatChangesAFilesModeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("note.md", "changed content", FileMode.EXECUTABLE_FILE),
                new NotebookGitProposalFile(
                    "README.md", "---\ntype: Readme\n---\nreadme original")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);
  }
}
