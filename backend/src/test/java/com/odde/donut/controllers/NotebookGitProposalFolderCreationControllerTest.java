package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Verifies publication of locally authored folders and their exact Git representation. */
class NotebookGitProposalFolderCreationControllerTest
    extends NotebookGitProposalFolderControllerTestBase {

  private static final String EDITED_EXISTING_WITH_LINK =
      "---\ntype: Note\n---\nExisting learned content linking [[Added]].\n";
  private static final String ADDED_INITIAL = "---\ntype: Note\n---\nInitial added body.\n";
  private static final String ADDED_FINAL = "---\ntype: Note\n---\nFinal added body.\n";

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void publishesMixedAddAndEditRangeWithFolderReadmeAndFinalAddedContent(boolean nestedDestination)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(EXISTING_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(existing.getId()).orElseThrow())
                    .spelling()
                    .please());
    Folder nestedParent = null;
    if (nestedDestination) {
      nestedParent = makeMe.aFolder().notebook(notebook).name("Parent").please();
      makeMe
          .aNote()
          .folder(nestedParent)
          .title("Keeper")
          .content("---\ntype: Note\n---\nKeeper body.\n")
          .please();
    }
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String folderReadmePath =
        nestedDestination ? "Parent/Child/README.md" : "Field Notes/README.md";
    String addedNotePath = nestedDestination ? "Parent/Child/Added.md" : "Field Notes/Added.md";
    String keeperPath = "Parent/Keeper.md";
    String keeperContent = "---\ntype: Note\n---\nKeeper body.\n";
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      List<NotebookGitProposalFile> afterB = new ArrayList<>();
      afterB.add(new NotebookGitProposalFile("Existing.md", EDITED_EXISTING_WITH_LINK));
      if (nestedDestination) {
        afterB.add(new NotebookGitProposalFile(keeperPath, keeperContent));
      }
      afterB.add(new NotebookGitProposalFile(folderReadmePath, FOLDER_README));
      afterB.add(new NotebookGitProposalFile(addedNotePath, ADDED_INITIAL));
      ObjectId afterAddAndEdit =
          commitOnTopOf(repository, List.of(acceptedHead), afterB, "Add folder and edit existing");
      List<NotebookGitProposalFile> afterC = new ArrayList<>();
      afterC.add(new NotebookGitProposalFile("Existing.md", EDITED_EXISTING_WITH_LINK));
      if (nestedDestination) {
        afterC.add(new NotebookGitProposalFile(keeperPath, keeperContent));
      }
      afterC.add(new NotebookGitProposalFile(folderReadmePath, FOLDER_README));
      afterC.add(new NotebookGitProposalFile(addedNotePath, ADDED_FINAL));
      tip = commitOnTopOf(repository, List.of(afterAddAndEdit), afterC, "Edit newly added note");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    Note reloadedExisting = noteRepository.findById(existing.getId()).orElseThrow();
    assertThat(reloadedExisting.getContent(), equalTo(EDITED_EXISTING_WITH_LINK));
    MemoryTracker retained = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(retained.getNote().getId(), equalTo(existing.getId()));
    Folder created =
        folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(folder -> folder.getName().equals(nestedDestination ? "Child" : "Field Notes"))
            .findFirst()
            .orElseThrow();
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    if (nestedParent == null) {
      assertThat(created.getParentFolderId(), nullValue());
    } else {
      assertThat(created.getParentFolderId(), equalTo(nestedParent.getId()));
    }
    Note added =
        noteByTitle(
            noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), "Added");
    assertThat(added.getContent(), equalTo(ADDED_FINAL));
    assertThat(added.getFolder().getId(), equalTo(created.getId()));
    NoteRealm existingView =
        inCommittedTransaction(
            transactionManager,
            () -> {
              try {
                return noteController.showNote(
                    noteRepository.findById(existing.getId()).orElseThrow());
              } catch (UnexpectedNoAccessRightException exception) {
                throw new IllegalStateException(exception);
              }
            });
    assertThat(existingView.getWikiLinks(), hasSize(1));
    assertThat(existingView.getWikiLinks().getFirst().getAuthoredLink(), equalTo("Added"));
    assertThat(
        existingView.getWikiLinks().getFirst().getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(
        existingView.getWikiLinks().getFirst().getDestinationNoteId(), equalTo(added.getId()));
  }

  @Test
  void publishesANewRootFolderAsTheExactAuthoredCommitAndMakesItDownloadable() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Field Notes/README.md", FOLDER_README)));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(1));
    Folder created = folders.getFirst();
    assertThat(created.getName(), equalTo("Field Notes"));
    assertThat(created.getParentFolderId(), nullValue());
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void preservesExistingRepresentedFolderIdentityWhenPublishingANewRootFolderReadme()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("Physics").please();
    Note motion =
        makeMe
            .aNote()
            .folder(physics)
            .title("Motion")
            .content("---\ntype: Note\n---\nExisting content.\n")
            .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "Physics/Motion.md", "---\ntype: Note\n---\nExisting content.\n"),
                new NotebookGitProposalFile("Field Notes/README.md", FOLDER_README)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(2));
    assertThat(
        noteRepository.findById(motion.getId()).orElseThrow().getFolder().getId(),
        equalTo(physics.getId()));
    Folder created =
        folders.stream()
            .filter(folder -> folder.getName().equals("Field Notes"))
            .findFirst()
            .orElseThrow();
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
  }

  @Test
  void rejectsFolderCreationWhenAnEmptyFolderAppearedOutsideAcceptedHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    Folder live = makeMe.aFolder().notebook(notebook).name("Field Notes").please();
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Field Notes/README.md", FOLDER_README)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposalBytes, HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("differs from accepted main"));
    assertThat(
        folderRepository.findById(live.getId()).orElseThrow().getReadmeContent(), nullValue());
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(1));
  }
}
