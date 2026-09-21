package com.odde.donut.controllers;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies an exact folder relocation constructs missing destination ancestry for a synchronized
 * source, still refuses a pre-existing unrepresented dest parent, and does not invent folder
 * identity. Represented parents are accepted in {@link
 * NotebookGitProposalFolderRelocationControllerTest}. Unrepresented source descendants stay in
 * {@link NotebookGitProposalFolderRelocationEmptyDescendantControllerTest}.
 */
class NotebookGitProposalFolderRelocationDestinationControllerTest
    extends NotebookGitControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = PortableTreeReadmeMarkdown.assemble(README_BODY);
  private static final String NOTE = "---\ntype: Note\n---\nnote";
  private static final String UNREPRESENTED_NOTE = "note";

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesAnExactSubtreeMoveIntoMissingTrashAncestryCreatingParentsAndRetainingIdentities()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = synchronizedBiology(notebook);
    Folder empty = makeMe.aFolder().parentFolder(biology).name("Empty").please();
    Note cells = makeMe.aNote().folder(biology).title("Cells").content(NOTE).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String destPrefix = "_trash/Research/Biology";
    byte[] proposalBytes = proposalBundleBytes(binding, biologySubtreeAt(destPrefix));
    GitBundleTestReader.SingleParentGitCommit proposedCommit = proposedCommit(proposalBytes);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    inCommittedTransaction(
        transactionManager,
        () -> {
          Map<Integer, Folder> folders = foldersById(notebook);
          Folder relocatedBiology = folders.get(biology.getId());
          Folder research = folders.get(relocatedBiology.getParentFolderId());
          Folder trash = folders.get(research.getParentFolderId());
          assertThat(research.getName(), equalTo("Research"));
          assertThat(trash.getName(), equalTo("_trash"));
          assertThat(trash.getParentFolderId(), nullValue());
          assertThat(folders.get(empty.getId()).getParentFolderId(), equalTo(biology.getId()));
          assertThat(
              noteRepository.findById(cells.getId()).orElseThrow().getFolder().getId(),
              equalTo(biology.getId()));
        });
    assertExactAcceptedTree(
        notebook, proposedCommit, publishedHead, binding, biologySubtreePaths(destPrefix));
  }

  @Test
  void publishesAFolderRecoveryIntoMissingActiveAncestryCreatingParents() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = synchronizedBiology(notebook);
    makeMe.aFolder().parentFolder(biology).name("Empty").please();
    makeMe.aNote().folder(biology).title("Cells").content(NOTE).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String destPrefix = "Research/Biology";
    byte[] proposalBytes = proposalBundleBytes(binding, biologySubtreeAt(destPrefix));
    GitBundleTestReader.SingleParentGitCommit proposedCommit = proposedCommit(proposalBytes);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    inCommittedTransaction(
        transactionManager,
        () -> {
          Folder relocatedBiology = folderRepository.findById(biology.getId()).orElseThrow();
          Folder research =
              folderRepository.findById(relocatedBiology.getParentFolderId()).orElseThrow();
          assertThat(research.getName(), equalTo("Research"));
          assertThat(research.getParentFolderId(), nullValue());
        });
    assertExactAcceptedTree(
        notebook, proposedCommit, publishedHead, binding, biologySubtreePaths(destPrefix));
  }

  @Test
  void rejectsAnExactFolderRelocationIntoAnExistingUnrepresentedParent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    Folder empty = makeMe.aFolder().notebook(notebook).name("Empty").please();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, topicsAtRoot());

    ResponseStatusException exception =
        publishRejected(
            notebook,
            binding,
            List.of(
                ofText("README.md", README_BODY),
                ofText("Empty/Topics/README.md", README_BODY),
                ofText("Empty/Topics/A.md", UNREPRESENTED_NOTE)));

    assertThat(exception.getReason(), containsString("Empty/Topics/README.md"));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Folder::getId)
                    .toList(),
                containsInAnyOrder(topics.getId(), empty.getId())));
  }

  private ResponseStatusException publishRejected(
      Notebook notebook, NotebookGitBinding binding, List<PortableTreeEntry> proposed)
      throws Exception {
    return assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(binding, NotebookGitProposalFile.asProposal(proposed)),
        HttpStatus.BAD_REQUEST);
  }

  private static List<PortableTreeEntry> topicsAtRoot() {
    return List.of(
        ofText("README.md", README_BODY),
        ofText("Topics/README.md", README_BODY),
        ofText("Topics/A.md", UNREPRESENTED_NOTE));
  }

  private Folder synchronizedBiology(Notebook notebook) {
    return makeMe.aFolder().notebook(notebook).name("Biology").readmeContent(README_BODY).please();
  }

  private static List<NotebookGitProposalFile> biologySubtreeAt(String destPrefix) {
    return List.of(
        new NotebookGitProposalFile(destPrefix + "/README.md", README),
        new NotebookGitProposalFile(destPrefix + "/Cells.md", NOTE),
        new NotebookGitProposalFile(destPrefix + "/Empty/.keep", ""));
  }

  private static List<String> biologySubtreePaths(String destPrefix) {
    return biologySubtreeAt(destPrefix).stream().map(NotebookGitProposalFile::path).toList();
  }

  private static GitBundleTestReader.SingleParentGitCommit proposedCommit(byte[] proposalBytes)
      throws Exception {
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      return GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }
  }

  private Map<Integer, Folder> foldersById(Notebook notebook) {
    return folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
        .collect(Collectors.toMap(Folder::getId, Function.identity()));
  }

  private void assertExactAcceptedTree(
      Notebook notebook,
      GitBundleTestReader.SingleParentGitCommit proposedCommit,
      String publishedHead,
      NotebookGitBinding parentBinding,
      List<String> expectedPaths)
      throws Exception {
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(
          downloadedCommit.parent().getName(), equalTo(parentBinding.getAcceptedGitObjectId()));
      assertThat(
          GitBundleTestReader.pathsIn(readBack, downloadedCommit.head()),
          containsInAnyOrder(expectedPaths.toArray(String[]::new)));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), expectedPaths.getLast()),
          equalTo(""));
    }
  }
}
