package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Verifies {@code publishNotebookGitProposal} accepts the one rename shape the tree-shape gate
 * recognizes: an isolated same-parent, equal-content remove/add pair, retaining the original note's
 * identity under its filename-derived title. Cross-parent and content-changed pairs remain covered
 * as rejections in {@link NotebookGitProposalTreeShapeControllerTest}.
 */
class NotebookGitProposalRenameControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Test
  void acceptsASameParentRenameRetainingTheOriginalNoteIdentity() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("renamed.md", TYPED_NOTE_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note renamed = notes.getFirst();
    assertThat(renamed.getId(), equalTo(note.getId()));
    assertThat(renamed.getTitle(), equalTo("renamed"));
    assertThat(renamed.getContent(), equalTo(TYPED_NOTE_CONTENT));
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

  @Test
  void acceptsASameParentRenameInsideANestedFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Folder").please();
    Note note = makeMe.aNote().folder(folder).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Folder/renamed.md", TYPED_NOTE_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note renamed = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(renamed.getTitle(), equalTo("renamed"));
    assertThat(renamed.getFolder().getId(), equalTo(folder.getId()));
    assertThat(renamed.getContent(), equalTo(TYPED_NOTE_CONTENT));
  }
}
