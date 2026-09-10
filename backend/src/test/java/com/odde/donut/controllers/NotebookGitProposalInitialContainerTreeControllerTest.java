package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies initial container Readme trees across Folder counts and depths. */
class NotebookGitProposalInitialContainerTreeControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String NOTEBOOK_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved notebook readme.\n";
  private static final String FOLDER_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved folder readme.\n";

  @Autowired FolderRepository folderRepository;

  @ParameterizedTest
  @CsvSource({
    "Parent/Child, false",
    "A|B|C, false",
    "A|B|C, true",
    "Parent/Child/Deep|Parent/Other|Sibling/Leaf, false",
    "Parent/Child/Deep|Parent/Other|Sibling/Leaf, true"
  })
  void publishesContainerTreesAsTheExactAuthoredCommit(String folderPaths, boolean notebookReadme)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    Map<String, String> documents = new LinkedHashMap<>();
    if (notebookReadme) documents.put("README.md", NOTEBOOK_README);
    Set<String> expectedFolders = new TreeSet<>();
    for (String path : folderPaths.split("\\|")) {
      documents.put(path + "/README.md", FOLDER_README + path + "\n");
      String prefix = path;
      while (true) {
        expectedFolders.add(prefix);
        int slash = prefix.lastIndexOf('/');
        if (slash < 0) break;
        prefix = prefix.substring(0, slash);
      }
    }
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            documents.entrySet().stream()
                .map(entry -> new NotebookGitProposalFile(entry.getKey(), entry.getValue()))
                .toList());
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }
    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), equalTo(documents.get("README.md")));
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    Map<Integer, String> pathsById = new LinkedHashMap<>();
    for (Folder folder : folders) {
      String path =
          folder.getParentFolderId() == null
              ? folder.getName()
              : pathsById.get(folder.getParentFolderId()) + "/" + folder.getName();
      pathsById.put(folder.getId(), path);
      assertThat(folder.getReadmeContent(), equalTo(documents.get(path + "/README.md")));
    }
    assertThat(folders, hasSize(expectedFolders.size()));
    assertThat(new TreeSet<>(pathsById.values()), equalTo(expectedFolders));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(
          GitBundleTestReader.pathsIn(readBack, downloadedCommit.head()),
          equalTo(documents.keySet().stream().sorted().toList()));
      for (Map.Entry<String, String> document : documents.entrySet()) {
        assertThat(
            new String(
                readBack
                    .open(
                        GitBundleTestReader.blobIdAt(
                            readBack, downloadedCommit.head(), document.getKey()))
                    .getBytes(),
                StandardCharsets.UTF_8),
            equalTo(document.getValue()));
      }
    }
  }
}
