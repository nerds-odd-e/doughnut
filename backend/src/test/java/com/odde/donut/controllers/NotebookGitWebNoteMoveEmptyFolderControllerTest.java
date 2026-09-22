package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Empty-folder proof: Biology contains only Cells (no Readme) and Study is an existing empty folder
 * represented by {@code Study/.keep}. The web Move appends B whose tree carries {@code
 * Biology/.keep} (Biology is now empty), {@code Study/Cells.md} (Study now has the note), and no
 * {@code Study/.keep} (Study is no longer empty). Both existing folders retain their identities; a
 * subsequent same-path local edit C published through the real controller keeps both original
 * folder identities and the note. {@link
 * com.odde.donut.services.notebookGit.NotebookGitTreeEncoder} emits {@code .keep} for empty folders
 * and omits it for non-empty ones.
 */
class NotebookGitWebNoteMoveEmptyFolderControllerTest extends NotebookGitWebNoteMoveTestBase {

  @Autowired FolderRepository folderRepository;

  @Test
  void webMovePreservesEmptyFolderMarkersAndOriginalFolderIdentitiesAcrossLocalPublication()
      throws UnexpectedNoAccessRightException, Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyOnlyWithEmptyStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));

    relationController.moveNoteToFolder(f.cells(), f.study());

    assertThat(reloadNote(f.cells()).getFolder().getId(), equalTo(f.study().getId()));
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
          containsInAnyOrder("Biology/.keep", "Study/Cells.md"));
      assertThat(GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Study/.keep")));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Biology/Cells.md")));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Study/Cells.md"),
          equalTo(CELLS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Biology/.keep"), equalTo(""));
    }

    assertThat(
        folderRepository.findById(f.biology().getId()).orElseThrow().getId(),
        equalTo(f.biology().getId()));
    assertThat(
        folderRepository.findById(f.study().getId()).orElseThrow().getId(),
        equalTo(f.study().getId()));

    NotebookGitBinding afterMove = binding(f.notebook());
    byte[] downloadedAfterMove = controller.downloadNotebookGitBundle(f.notebook()).getBody();
    byte[] proposalC = proposalModifyingPath(downloadedAfterMove, "Study/Cells.md", EDITED_BODY);
    controller.publishNotebookGitProposal(
        f.notebook().getId(), afterMove.getAcceptedGitObjectId(), proposalC);

    assertShownContentAndRetainedLearning(f.cells(), f.tracker(), EDITED_BODY);
    assertThat(countRecallPromptsByNoteId(f.cells().getId()), equalTo(f.recallCountBefore()));
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(f.notebook().getId()).size(), is(2));
    assertThat(
        folderRepository.findById(f.biology().getId()).orElseThrow().getId(),
        equalTo(f.biology().getId()));
    assertThat(
        folderRepository.findById(f.study().getId()).orElseThrow().getId(),
        equalTo(f.study().getId()));
    assertThat(reloadNote(f.cells()).getFolder().getId(), equalTo(f.study().getId()));
  }

  /**
   * Slice 5 fixture: a Git-backed notebook where Biology contains ONLY Cells (no Readme) and Study
   * is an existing empty folder with no Readme, so the accepted A tree carries {@code Study/.keep}
   * and {@code Biology/Cells.md} but no {@code Biology/.keep}. After moving Cells to Study, Biology
   * becomes empty (needs {@code Biology/.keep}) and Study gets Cells (drops {@code Study/.keep}).
   */
  private LearnedMoveFixture seedLearnedCellsInBiologyOnlyWithEmptyStudyDestination()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder study = makeMe.aFolder().notebook(notebook).name("Study").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    long recallCountBefore = countRecallPromptsByNoteId(cells.getId());
    snapshotCurrentPortableTree(notebook);
    return new LearnedMoveFixture(
        notebook, biology, study, cells, tracker, recallCountBefore, currentUser.getUser());
  }

  /**
   * Builds a single-parent proposal on top of the accepted head in {@code acceptedBundleBytes} that
   * preserves every file already in the accepted tree and overrides exactly one path's content —
   * mirroring a local checkout edit followed by a commit on the moved note's new path.
   */
  private byte[] proposalModifyingPath(byte[] acceptedBundleBytes, String path, String content)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, acceptedBundleBytes);
      ObjectId parentTree;
      try (RevWalk revWalk = new RevWalk(repository)) {
        parentTree = revWalk.parseCommit(acceptedHead).getTree();
      }
      try (ObjectInserter inserter = repository.newObjectInserter()) {
        DirCache dirCache = DirCache.newInCore();
        DirCacheBuilder builder = dirCache.builder();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
          treeWalk.addTree(parentTree);
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            if (treeWalk.getPathString().equals(path)) {
              continue;
            }
            DirCacheEntry entry = new DirCacheEntry(treeWalk.getPathString());
            entry.setFileMode(treeWalk.getFileMode(0));
            entry.setObjectId(treeWalk.getObjectId(0));
            builder.add(entry);
          }
        }
        ObjectId blobId =
            inserter.insert(Constants.OBJ_BLOB, content.getBytes(StandardCharsets.UTF_8));
        DirCacheEntry override = new DirCacheEntry(path);
        override.setFileMode(FileMode.REGULAR_FILE);
        override.setObjectId(blobId);
        builder.add(override);
        builder.finish();
        ObjectId treeId = dirCache.writeTree(inserter);
        inserter.flush();
        org.eclipse.jgit.lib.PersonIdent author =
            new org.eclipse.jgit.lib.PersonIdent("Proposer", "proposer@example.com");
        org.eclipse.jgit.lib.CommitBuilder commitBuilder = new org.eclipse.jgit.lib.CommitBuilder();
        commitBuilder.setTreeId(treeId);
        commitBuilder.setParentIds(acceptedHead);
        commitBuilder.setAuthor(author);
        commitBuilder.setCommitter(author);
        commitBuilder.setMessage("Proposal");
        ObjectId commitId = inserter.insert(commitBuilder);
        inserter.flush();
        return bundleBytesForHead(repository, commitId);
      }
    }
  }
}
