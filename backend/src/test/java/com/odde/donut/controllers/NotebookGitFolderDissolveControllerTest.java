package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

class NotebookGitFolderDissolveControllerTest extends NotebookGitWebContentControllerTestBase {

  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String BIOLOGY_README = "Biology readme";
  static final String REFERRER_BODY_BEFORE =
      "---\ntype: Note\nrelated: \"[[Outer/Biology/Cells|shown]]\"\n---\nSee "
          + "[[Outer/Biology/Cells|shown]] for details.";
  static final String REFERRER_BODY_AFTER =
      "---\ntype: Note\nrelated: \"[[Outer/Cells|shown]]\"\n---\nSee [[Outer/Cells|shown]] for "
          + "details.";

  @Test
  void webFolderDissolvePromotesChildrenIntoOneAcceptedChildAndRetainsIdentity() throws Exception {
    CompleteDissolveFixture f = seedBiologyUnderOuterWithReferrer();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());

    folderController.dissolveFolder(f.notebook(), f.biology(), false);

    assertShownContentAndRetainedLearning(f.cells(), f.tracker(), CELLS_BODY);
    assertThat(countRecallPromptsByNoteId(f.cells().getId()), equalTo(f.recallCountBefore()));

    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead = fetchDownloadedHead(repo, f.notebook());
      assertThat(downloadedHead, equalTo(acceptedB));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commitB = revWalk.parseCommit(downloadedHead);
        assertThat(commitB.getParentCount(), is(1));
        assertThat(commitB.getParent(0).getId(), equalTo(acceptedA));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder("Outer/Cells.md", "Outer/Empty/.keep", "Reading.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Outer/Cells.md"),
          equalTo(CELLS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Reading.md"),
          equalTo(REFERRER_BODY_AFTER));
    }
  }

  @Test
  void dissolveMergeRequestedResultsInOneMergedAcceptedChild() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    Folder outerSame = makeMe.aFolder().parentFolder(outer).name("Same").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    Folder midSame = makeMe.aFolder().parentFolder(mid).name("Same").please();
    Note midNote = makeMe.aNote("MidNote").folder(midSame).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    folderController.dissolveFolder(notebook, mid, true);
    Note reloadedMidNote = noteRepository.findById(midNote.getId()).orElseThrow();

    assertThat(reloadedMidNote.getFolder().getId(), equalTo(outerSame.getId()));
    ObjectId acceptedB = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    assertThat(acceptedB, is(not(acceptedA)));
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead = fetchDownloadedHead(repo, notebook);
      assertThat(downloadedHead, equalTo(acceptedB));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder("Outer/Same/MidNote.md"));
    }
  }

  @Test
  void dissolveCarriesAFileIntoTheParentWithTheSameBytes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder old = makeMe.aFolder().parentFolder(physics).name("old").please();
    NotebookAttachment sketch =
        storeFolderAttachmentAndSnapshot(notebook, old, "sketch.png", new byte[] {1, 2, 3});

    folderController.dissolveFolder(notebook, old, false);

    assertThat(acceptedHistory(notebook).tipPaths(), containsInAnyOrder("physics/sketch.png"));
    assertThat(
        acceptedBytesAt(notebook, "physics/sketch.png"),
        equalTo(
            notebookAttachmentRepository
                .findById(sketch.getId())
                .orElseThrow()
                .getAcceptedGitContent()));
  }

  @Test
  void dissolveWithMergeCarriesAFileIntoTheSameNamedFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder diagrams = makeMe.aFolder().parentFolder(physics).name("diagrams").please();
    storeFolderAttachmentAndSnapshot(notebook, diagrams, "a.png", new byte[] {1});
    Folder old = makeMe.aFolder().parentFolder(physics).name("old").please();
    Folder oldDiagrams = makeMe.aFolder().parentFolder(old).name("diagrams").please();
    storeFolderAttachmentAndSnapshot(notebook, oldDiagrams, "b.png", new byte[] {2});

    folderController.dissolveFolder(notebook, old, true);

    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder("physics/diagrams/a.png", "physics/diagrams/b.png"));
  }

  @Test
  void dissolvingLastEmptyChildLeavesParentKeepMarker() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    Folder empty = makeMe.aFolder().parentFolder(outer).name("Empty").please();
    snapshotCurrentPortableTree(notebook);

    folderController.dissolveFolder(notebook, empty, false);

    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead = fetchDownloadedHead(repo, notebook);
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), containsInAnyOrder("Outer/.keep"));
    }
  }

  private ObjectId fetchDownloadedHead(InMemoryRepository repo, Notebook notebook)
      throws Exception {
    return GitBundleTestReader.fetchHead(
        repo,
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody());
  }

  CompleteDissolveFixture seedBiologyUnderOuterWithReferrer()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    Folder biology =
        makeMe.aFolder().parentFolder(outer).name("Biology").readmeContent(BIOLOGY_README).please();
    makeMe.aFolder().parentFolder(biology).name("Empty").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    long recallCountBefore = countRecallPromptsByNoteId(cells.getId());
    Note referrer = makeMe.aNote("Reading").notebook(notebook).please();
    authorReferencingContentCommitted(referrer, REFERRER_BODY_BEFORE);
    snapshotCurrentPortableTree(notebook);
    return new CompleteDissolveFixture(notebook, biology, cells, tracker, recallCountBefore);
  }

  record CompleteDissolveFixture(
      Notebook notebook,
      Folder biology,
      Note cells,
      MemoryTracker tracker,
      long recallCountBefore) {}
}
