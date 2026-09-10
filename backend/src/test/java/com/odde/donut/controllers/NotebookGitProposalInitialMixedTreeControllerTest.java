package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies mixed initial trees with explicit and implied Folder ancestry. */
class NotebookGitProposalInitialMixedTreeControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String NOTEBOOK_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved notebook readme.\n";
  private static final String FOLDER_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved folder readme.\n";

  @Autowired FolderRepository folderRepository;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void publishesMixedConceptPlacementsAsTheExactAuthoredTree(boolean deeper) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    Map<String, String> documents = new LinkedHashMap<>();
    if (deeper) {
      documents.put("Root.md", "---\ntype: Note\n---\nRoot body.\n");
      documents.put("Parent/README.md", FOLDER_README);
      documents.put(
          "Parent/Child/Deep/Custom.md", "---\ntype: CustomType\ncustom: value\n---\nDeep body.\n");
      documents.put("Parent/Other/A.md", "---\ntype: Note\n---\nA body.\n");
      documents.put("Sibling/B.md", "---\ntype: Note\n---\nB body.\n");
    } else {
      documents.put("README.md", NOTEBOOK_README);
      documents.put("Topic/A.md", "---\ntype: Note\n---\nA body.\n");
      documents.put("Topic/B.md", "---\ntype: Note\n---\nB body.\n");
      documents.put(
          "Topic/A-related-to-B.md",
          "---\ntype: Relationship\nrelation: related-to\nsource: \"[[Topic/A]]\"\ntarget: \"[[Topic/B]]\"\n---\nRelation body.\n");
    }
    Set<String> expectedFolders =
        deeper
            ? Set.of("Parent", "Parent/Child", "Parent/Child/Deep", "Parent/Other", "Sibling")
            : Set.of("Topic");
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
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    Map<String, String> conceptDocuments = new LinkedHashMap<>();
    for (Note note : notes) {
      String prefix = note.getFolder() == null ? "" : pathsById.get(note.getFolder().getId()) + "/";
      conceptDocuments.put(prefix + note.getTitle() + ".md", note.getContent());
    }
    Map<String, String> expectedConcepts = new LinkedHashMap<>(documents);
    expectedConcepts
        .keySet()
        .removeIf(path -> path.equals("README.md") || path.endsWith("/README.md"));
    assertThat(conceptDocuments, equalTo(expectedConcepts));
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
