package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies {@code publishNotebookGitProposal} accepts one exact folder relocation by reparenting
 * the existing source Folder. Shape, destination, placement, and empty-descendant refusals stay in
 * their dedicated classes. Private-association retention is covered in {@link
 * NotebookGitProposalFolderRelocationPrivateAssociationControllerTest}. Note relocation, including
 * a last-note move that keeps its container, is covered in {@link
 * NotebookGitProposalRelocationControllerTest} and {@link
 * NotebookGitProposalRelocationContainerControllerTest}.
 */
class NotebookGitProposalFolderRelocationControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = ExportReadmeMarkdown.assemble(README_BODY);
  private static final String NOTE = "---\ntype: Note\n---\nnote";

  @Autowired FolderRepository folderRepository;

  @Test
  void acceptsAnExactFolderRelocationRetainingSourceAndDescendantIdentities() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    Folder sub = makeMe.aFolder().parentFolder(topics).name("Sub").please();
    Note nested = makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    Note deeper = makeMe.aNote().folder(sub).title("B").content(NOTE).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Archive/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/A.md", NOTE),
                new NotebookGitProposalFile("Archive/Topics/Sub/B.md", NOTE)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Map<Integer, Folder> folders = foldersById(notebook);
    assertThat(folders.keySet(), containsInAnyOrder(archive.getId(), topics.getId(), sub.getId()));
    assertThat(folders.get(archive.getId()).getParentFolderId(), nullValue());
    assertThat(folders.get(topics.getId()).getParentFolderId(), equalTo(archive.getId()));
    assertThat(folders.get(sub.getId()).getParentFolderId(), equalTo(topics.getId()));
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(
        notes.stream().map(Note::getId).toList(),
        containsInAnyOrder(nested.getId(), deeper.getId()));
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

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"root", "nested", "readme-only-source", "descendant-only"})
  void acceptsAnExactFolderRelocationIntoARepresentedParent(String scenario) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Placement placement = seedPlacement(notebook, scenario);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes = proposalBundleBytes(binding, placement.proposed());

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Folder relocated = foldersById(notebook).get(placement.source().getId());
    assertThat(relocated.getParentFolderId(), equalTo(placement.destParentId()));
  }

  @Test
  void retriesAnAcceptedFolderRelocationWithoutChangingParentIdsOrBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Placement placement = seedPlacement(notebook, "nested");
    NotebookGitBinding initialBinding = snapshotCurrentPortableTree(notebook);
    String initialHead = initialBinding.getAcceptedGitObjectId();
    byte[] proposalBytes = proposalBundleBytes(initialBinding, placement.proposed());

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);
    PublicationState stateAfterPublication = publicationState(notebook, placement.source().getId());
    assertThat(stateAfterPublication.sourceParentId(), equalTo(placement.destParentId()));

    String retriedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    PublicationState stateAfterRetry = publicationState(notebook, placement.source().getId());
    assertThat(stateAfterRetry.acceptedHead(), equalTo(stateAfterPublication.acceptedHead()));
    assertThat(
        stateAfterRetry.bindingUpdatedAt(), equalTo(stateAfterPublication.bindingUpdatedAt()));
    assertThat(stateAfterRetry.bundleBytes(), equalTo(stateAfterPublication.bundleBytes()));
    assertThat(stateAfterRetry.sourceParentId(), equalTo(stateAfterPublication.sourceParentId()));
    assertThat(stateAfterRetry.folderIds(), equalTo(stateAfterPublication.folderIds()));
    assertThat(stateAfterRetry.noteIds(), equalTo(stateAfterPublication.noteIds()));
  }

  private PublicationState publicationState(Notebook notebook, Integer sourceFolderId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          Folder source = folderRepository.findById(sourceFolderId).orElseThrow();
          return new PublicationState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              binding.getBundleBytes().clone(),
              source.getParentFolderId(),
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Folder::getId)
                  .toList(),
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Note::getId)
                  .toList());
        });
  }

  private Placement seedPlacement(Notebook notebook, String scenario) {
    if ("root".equals(scenario)) {
      Folder archive =
          makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
      Folder topics =
          makeMe.aFolder().parentFolder(archive).name("Topics").readmeContent(README_BODY).please();
      makeMe.aNote().folder(topics).title("A").content(NOTE).please();
      return new Placement(
          topics,
          null,
          List.of(
              new NotebookGitProposalFile("Archive/README.md", README),
              new NotebookGitProposalFile("Topics/README.md", README),
              new NotebookGitProposalFile("Topics/A.md", NOTE)));
    }
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    if ("readme-only-source".equals(scenario)) {
      Folder archive =
          makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
      return new Placement(
          topics,
          archive.getId(),
          List.of(
              new NotebookGitProposalFile("Archive/README.md", README),
              new NotebookGitProposalFile("Archive/Topics/README.md", README)));
    }
    if ("descendant-only".equals(scenario)) {
      Folder courses = makeMe.aFolder().notebook(notebook).name("Courses").please();
      Folder physics = makeMe.aFolder().parentFolder(courses).name("Physics").please();
      makeMe.aNote().folder(topics).title("A").content(NOTE).please();
      makeMe.aNote().folder(physics).title("Motion").content(NOTE).please();
      return new Placement(
          topics,
          courses.getId(),
          List.of(
              new NotebookGitProposalFile("Courses/Physics/Motion.md", NOTE),
              new NotebookGitProposalFile("Courses/Topics/README.md", README),
              new NotebookGitProposalFile("Courses/Topics/A.md", NOTE)));
    }
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
    makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    return new Placement(
        topics,
        archive.getId(),
        List.of(
            new NotebookGitProposalFile("Archive/README.md", README),
            new NotebookGitProposalFile("Archive/Topics/README.md", README),
            new NotebookGitProposalFile("Archive/Topics/A.md", NOTE)));
  }

  private Map<Integer, Folder> foldersById(Notebook notebook) {
    return folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
        .collect(Collectors.toMap(Folder::getId, Function.identity()));
  }

  private record Placement(
      Folder source, Integer destParentId, List<NotebookGitProposalFile> proposed) {}

  private record PublicationState(
      String acceptedHead,
      Timestamp bindingUpdatedAt,
      byte[] bundleBytes,
      Integer sourceParentId,
      List<Integer> folderIds,
      List<Integer> noteIds) {}
}
