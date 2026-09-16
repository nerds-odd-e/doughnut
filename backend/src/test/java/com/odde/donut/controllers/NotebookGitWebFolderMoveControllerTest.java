package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebFolderMoveControllerTest extends NotebookGitWebContentControllerTestBase {

  static final Instant MOVE_AT = Instant.parse("2026-09-16T06:00:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  @Autowired FolderRepository folderRepository;

  @Test
  void webFolderMoveAppendsAcceptedChildAndRetainsNoteAndLearningIdentity() throws Exception {
    FolderMoveFixture f = seedLearnedCellsInBiologyWithEmptyStudy();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));

    controller.moveFolder(f.notebook(), f.biology(), folderMove(f.study().getId()));

    assertThat(parentFolderId(f.biology().getId()), equalTo(f.study().getId()));
    assertThat(
        noteRepository.findById(f.cells().getId()).orElseThrow().getFolder().getId(),
        equalTo(f.biology().getId()));
    assertShownContentAndRetainedLearning(f.cells(), f.tracker(), CELLS_BODY);
    assertThat(countRecallPromptsByNoteId(f.cells().getId()), equalTo(f.recallCountBefore()));

    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(downloadedHead, equalTo(acceptedB));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commitB = revWalk.parseCommit(downloadedHead);
        assertThat(commitB.getParentCount(), is(1));
        assertThat(commitB.getParent(0).getId(), equalTo(acceptedA));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder("Study/Biology/Cells.md"));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Biology/Cells.md")));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Study/Biology/Cells.md"),
          equalTo(CELLS_BODY));
    }
  }

  @Test
  void destinationCollisionLeavesPlacementAndAcceptedHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder study = makeMe.aFolder().notebook(notebook).name("Study").please();
    Folder occupyingBiology = makeMe.aFolder().parentFolder(study).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    ApiException conflict =
        assertThrows(
            ApiException.class,
            () -> controller.moveFolder(notebook, biology, folderMove(study.getId())));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(parentFolderId(biology.getId()), nullValue());
    assertThat(parentFolderId(occupyingBiology.getId()), equalTo(study.getId()));
    assertThat(
        noteRepository.findById(cells.getId()).orElseThrow().getFolder().getId(),
        equalTo(biology.getId()));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void nonGitNotebookFolderMoveCreatesNoBinding() throws UnexpectedNoAccessRightException {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder study = makeMe.aFolder().notebook(notebook).name("Study").please();

    controller.moveFolder(notebook, biology, folderMove(study.getId()));

    assertThat(parentFolderId(biology.getId()), equalTo(study.getId()));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isPresent(), is(false));
  }

  @Test
  void preExistingPortableDriftKeepsTheFolderMoveAndAcceptedHistoryUnchanged() throws Exception {
    FolderMoveFixture f = seedLearnedCellsInBiologyWithEmptyStudy();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    byte[] acceptedBundle = binding(f.notebook()).getBundleBytes();
    makeMe.aNote().notebook(f.notebook()).title("Unsynchronized").content(CELLS_BODY).please();

    controller.moveFolder(f.notebook(), f.biology(), folderMove(f.study().getId()));

    assertThat(parentFolderId(f.biology().getId()), equalTo(f.study().getId()));
    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedA));
    assertThat(binding(f.notebook()).getBundleBytes(), equalTo(acceptedBundle));
  }

  FolderMoveFixture seedLearnedCellsInBiologyWithEmptyStudy()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder study = makeMe.aFolder().notebook(notebook).name("Study").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    long recallCountBefore = countRecallPromptsByNoteId(cells.getId());
    snapshotCurrentPortableTree(notebook);
    return new FolderMoveFixture(notebook, biology, study, cells, tracker, recallCountBefore);
  }

  Integer parentFolderId(Integer folderId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Folder folder = folderRepository.findById(folderId).orElseThrow();
          return folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
        });
  }

  static FolderMoveRequest folderMove(Integer newParentFolderId) {
    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(newParentFolderId);
    return req;
  }

  record FolderMoveFixture(
      Notebook notebook,
      Folder biology,
      Folder study,
      Note cells,
      MemoryTracker tracker,
      long recallCountBefore) {}
}
