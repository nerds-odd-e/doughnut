package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies last-note deletion keeps the Donut notebook/folder and does not manufacture a README.
 */
class NotebookGitDeletionContainerPublicationControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String CONTENT = "---\ntype: Note\n---\nOnly authored bytes.\n";

  @Autowired FolderRepository folderRepository;

  @ParameterizedTest
  @MethodSource("lastNotePlacements")
  void publishesIsolatedDeletionOfTheLastNoteWithoutRemovingItsContainer(
      String folderName, String deletedPath) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    placeLastNote(notebook, folderName);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes = proposalBundleBytes(binding, List.of());

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook remaining = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(
        notebookRepository.findByOwnership_IdAndDeletedAtIsNull(remaining.getOwnership().getId()),
        hasSize(1));
    assertThat(remaining.getReadmeContent(), nullValue());
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(remaining.getId()), empty());
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(remaining.getId());
    if (folderName == null) {
      assertThat(folders, empty());
    } else {
      assertThat(folders, hasSize(1));
      assertThat(folders.getFirst().getName(), equalTo(folderName));
      assertThat(folders.getFirst().getReadmeContent(), nullValue());
    }

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(remaining);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
      assertThat(pathsIn(readBack, downloadedCommit.head()), empty());
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.parent(), deletedPath),
          equalTo(CONTENT));
    }
  }

  static Stream<Arguments> lastNotePlacements() {
    return Stream.of(
        Arguments.of((String) null, "Only.md"), Arguments.of("Physics", "Physics/Only.md"));
  }

  private void placeLastNote(Notebook notebook, String folderName) {
    if (folderName == null) {
      makeMe.aNote().notebook(notebook).title("Only").content(CONTENT).please();
      return;
    }
    Folder folder = makeMe.aFolder().notebook(notebook).name(folderName).please();
    makeMe.aNote().folder(folder).title("Only").content(CONTENT).please();
  }

  private static List<String> pathsIn(InMemoryRepository repository, ObjectId commitId)
      throws Exception {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(revWalk.parseCommit(commitId).getTree());
      treeWalk.setRecursive(true);
      List<String> paths = new ArrayList<>();
      while (treeWalk.next()) {
        paths.add(treeWalk.getPathString());
      }
      return paths;
    }
  }
}
