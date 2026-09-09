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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies publication of two sibling root Folder Readmes. */
class NotebookGitProposalInitialSiblingFolderReadmesControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String FOLDER_A_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved folder A readme.\n";
  private static final String FOLDER_B_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved folder B readme.\n";

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesTwoSiblingFolderReadmesAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Folder A/README.md", FOLDER_A_README),
                new NotebookGitProposalFile("Folder B/README.md", FOLDER_B_README)));

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
    Map<String, Folder> byName =
        folders.stream().collect(Collectors.toMap(Folder::getName, Function.identity()));
    Folder folderA = byName.get("Folder A");
    Folder folderB = byName.get("Folder B");
    assertThat(folderA.getParentFolderId(), nullValue());
    assertThat(folderA.getReadmeContent(), equalTo(FOLDER_A_README));
    assertThat(folderB.getParentFolderId(), nullValue());
    assertThat(folderB.getReadmeContent(), equalTo(FOLDER_B_README));
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
