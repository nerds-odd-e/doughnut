package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NotebookGitFolderRenameControllerTest extends NotebookGitWebContentControllerTestBase {

  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String BIOLOGY_README = "Biology readme";
  static final String REFERRER_BODY_BEFORE =
      "---\ntype: Note\nrelated: \"[[Biology/Cells|shown]]\"\n---\nSee [[Biology/Cells|shown]] for "
          + "details.";
  static final String REFERRER_BODY_AFTER =
      "---\ntype: Note\nrelated: \"[[Zoology/Cells|shown]]\"\n---\nSee [[Zoology/Cells|shown]] for "
          + "details.";
  static final String EDITED_CELLS_BODY = "---\ntype: Note\n---\nedited cells body";

  @Test
  void webFolderRenameAppendsAcceptedChildAndRetainsNoteAndLearningIdentity() throws Exception {
    CompleteRenameFixture f = seedCompleteBiologySubtreeWithReferrer();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());

    Folder renamed = folderController.renameFolder(f.notebook(), f.biology(), renameTo("Zoology"));

    assertThat(renamed.getName(), equalTo("Zoology"));
    assertShownContentAndRetainedLearning(f.cells(), f.tracker(), CELLS_BODY);
    assertThat(countRecallPromptsByNoteId(f.cells().getId()), equalTo(f.recallCountBefore()));

    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(repo, acceptedBundleBytes(f.notebook()));
      assertThat(downloadedHead, equalTo(acceptedB));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commitB = revWalk.parseCommit(downloadedHead);
        assertThat(commitB.getParentCount(), is(1));
        assertThat(commitB.getParent(0).getId(), equalTo(acceptedA));
      }
    }
  }

  @Test
  void webFolderRenameProjectsCompleteSubtreeAndRewrittenInNotebookReferences() throws Exception {
    CompleteRenameFixture f = seedCompleteBiologySubtreeWithReferrer();

    folderController.renameFolder(f.notebook(), f.biology(), renameTo("Zoology"));

    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(repo, acceptedBundleBytes(f.notebook()));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder(
              "Zoology/README.md", "Zoology/Cells.md", "Zoology/Empty/.keep", "Reading.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Zoology/README.md"),
          equalTo(ExportReadmeMarkdown.assemble(BIOLOGY_README)));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Zoology/Cells.md"),
          equalTo(CELLS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Zoology/Empty/.keep"),
          equalTo(""));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Reading.md"),
          equalTo(REFERRER_BODY_AFTER));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Biology", "  Biology  "})
  void noOpRenameLeavesFolderAndAcceptedHistoryUnchanged(String requestedName) throws Exception {
    CompleteRenameFixture f = seedCompleteBiologySubtreeWithReferrer();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    var acceptedHistoryBefore = acceptedHistory(f.notebook());

    Folder result =
        folderController.renameFolder(f.notebook(), f.biology(), renameTo(requestedName));

    assertThat(result.getName(), equalTo("Biology"));
    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedA));
    assertThat(acceptedHistory(f.notebook()), equalTo(acceptedHistoryBefore));
  }

  @Test
  void nonGitNotebookFolderRenameCreatesNoBinding() throws UnexpectedNoAccessRightException {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();

    Folder renamed = folderController.renameFolder(notebook, biology, renameTo("Zoology"));

    assertThat(renamed.getName(), equalTo("Zoology"));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isPresent(), is(false));
  }

  @Test
  void renameDoesNotStrandSubsequentContentEdit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);

    folderController.renameFolder(notebook, biology, renameTo("Zoology"));
    ObjectId renameHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    Note reloadedCells = noteRepository.findById(cells.getId()).orElseThrow();
    NoteRealm saved =
        textContentController.updateNoteContent(reloadedCells, contentDto(EDITED_CELLS_BODY));
    assertThat(saved.getId(), equalTo(cells.getId()));

    ObjectId editHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead = GitBundleTestReader.fetchHead(repo, acceptedBundleBytes(notebook));
      assertThat(downloadedHead, equalTo(editHead));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit editCommit = revWalk.parseCommit(downloadedHead);
        assertThat(editCommit.getParentCount(), is(1));
        assertThat(editCommit.getParent(0).getId(), equalTo(renameHead));
      }
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Zoology/Cells.md"),
          equalTo(EDITED_CELLS_BODY));
    }
  }

  CompleteRenameFixture seedCompleteBiologySubtreeWithReferrer()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology =
        makeMe.aFolder().notebook(notebook).name("Biology").readmeContent(BIOLOGY_README).please();
    makeMe.aFolder().parentFolder(biology).name("Empty").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    long recallCountBefore = countRecallPromptsByNoteId(cells.getId());
    Note referrer = makeMe.aNote("Reading").notebook(notebook).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_BODY_BEFORE));
    snapshotCurrentPortableTree(notebook);
    return new CompleteRenameFixture(notebook, biology, cells, tracker, recallCountBefore);
  }

  static FolderRenameRequest renameTo(String name) {
    FolderRenameRequest req = new FolderRenameRequest();
    req.setName(name);
    return req;
  }

  record CompleteRenameFixture(
      Notebook notebook,
      Folder biology,
      Note cells,
      MemoryTracker tracker,
      long recallCountBefore) {}
}
