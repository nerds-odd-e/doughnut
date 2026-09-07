package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies relocating a folder's last tracked note keeps the Donut source container and does not
 * manufacture a README. Placement is covered in {@link
 * NotebookGitProposalRelocationControllerTest}. Last-note deletion containers are covered in {@link
 * NotebookGitDeletionContainerPublicationControllerTest}.
 */
class NotebookGitProposalRelocationContainerControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesRelocationOfTheLastNoteWithoutRemovingItsSourceContainer() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(destination).title("other").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().folder(source).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Dest/other.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/note.md", TYPED_NOTE_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(2));
    Folder remainingSource =
        folders.stream()
            .filter(folder -> folder.getId().equals(source.getId()))
            .findFirst()
            .orElseThrow();
    Folder remainingDestination =
        folders.stream()
            .filter(folder -> folder.getId().equals(destination.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(remainingSource.getReadmeContent(), nullValue());
    assertThat(remainingDestination.getReadmeContent(), nullValue());

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
      assertThat(
          GitBundleTestReader.pathsIn(readBack, downloadedCommit.head()),
          containsInAnyOrder("Dest/note.md", "Dest/other.md"));
    }
  }
}
