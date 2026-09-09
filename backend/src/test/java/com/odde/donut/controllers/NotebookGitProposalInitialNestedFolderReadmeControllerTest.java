package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies publication of one nested Folder Readme ({@code Parent/Child/README.md}). */
class NotebookGitProposalInitialNestedFolderReadmeControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String CHILD_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved nested folder readme.\n";

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesOneNestedFolderReadmeAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Parent/Child/README.md", CHILD_README)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), nullValue());
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(2));
    Folder parent = folders.get(0);
    Folder child = folders.get(1);
    assertThat(parent.getName(), equalTo("Parent"));
    assertThat(parent.getParentFolderId(), nullValue());
    assertThat(parent.getReadmeContent(), nullValue());
    assertThat(child.getName(), equalTo("Child"));
    assertThat(child.getParentFolderId(), equalTo(parent.getId()));
    assertThat(child.getReadmeContent(), equalTo(CHILD_README));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }
}
