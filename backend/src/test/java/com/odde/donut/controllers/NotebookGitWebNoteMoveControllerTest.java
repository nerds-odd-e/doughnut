package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Slice 2 Git-history proof: a web Move in one synchronized Git-backed notebook appends the moved
 * tree to accepted history; a clean download receives the exact new path with the old path gone; a
 * subsequent local proposal published through the real controller updates the original note while
 * retaining its note/tracker/recall identity and scheduling. Covers queued save-after-move parent
 * ordering and projection-drift refusal on a stale pre-move head.
 */
class NotebookGitWebNoteMoveControllerTest extends NotebookGitWebNoteMoveTestBase {

  @Test
  void webMoveAppendsAcceptedChildAndLocalPublicationRetainsLearningHistory() throws Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));

    relationController.moveNoteToFolder(f.cells(), f.study());

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
          containsInAnyOrder("Biology/README.md", "Study/README.md", "Study/Cells.md"));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Biology/Cells.md")));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Study/Cells.md"),
          equalTo(CELLS_BODY));
    }

    NotebookGitBinding afterMove = binding(f.notebook());
    byte[] downloadedAfterMove = controller.downloadNotebookGitBundle(f.notebook()).getBody();
    byte[] proposalC = proposalModifyingPath(downloadedAfterMove, "Study/Cells.md", EDITED_BODY);
    controller.publishNotebookGitProposal(
        f.notebook().getId(), afterMove.getAcceptedGitObjectId(), proposalC);

    assertShownContentAndRetainedLearning(f.cells(), f.tracker(), EDITED_BODY);
    assertThat(countRecallPromptsByNoteId(f.cells().getId()), equalTo(f.recallCountBefore()));
  }

  @Test
  void queuedSaveAfterMoveAppendsBothRevisionsInParentOrder() throws Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());

    NotebookGitConcurrentWriterTestSupport.Result<List<NoteRealm>, NoteRealm> race =
        NotebookGitConcurrentWriterTestSupport.runInQueuedOrder(
            transactionManager,
            notebookGitBindingRepository,
            currentUser,
            currentUser.getUser(),
            f.notebook().getId(),
            () -> {
              testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));
              return relationController.moveNoteToFolder(
                  noteRepository.findById(f.cells().getId()).orElseThrow(), f.study());
            },
            () -> {
              testabilitySettings.timeTravelTo(Timestamp.from(SAVE_AFTER_MOVE_AT));
              return textContentController.updateNoteContent(
                  noteRepository.findById(f.cells().getId()).orElseThrow(),
                  contentDto(EDITED_BODY));
            });

    assertThat(race.first().get(0).getNote().getFolder().getId(), equalTo(f.study().getId()));
    assertThat(race.second().getNote().getContent(), equalTo(EDITED_BODY));
    byte[] bundleBytes = controller.downloadNotebookGitBundle(f.notebook()).getBody();
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repo, bundleBytes);
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit saveCommit = revWalk.parseCommit(head);
        assertThat(saveCommit.getParentCount(), is(1));
        RevCommit moveCommit = revWalk.parseCommit(saveCommit.getParent(0));
        assertThat(moveCommit.getParentCount(), is(1));
        assertThat(moveCommit.getParent(0).getId(), equalTo(acceptedA));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repo, saveCommit, "Study/Cells.md"),
            equalTo(EDITED_BODY));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repo, moveCommit, "Study/Cells.md"),
            equalTo(CELLS_BODY));
      }
    }
  }

  @Test
  void proposalOnStalePreMoveHeadIsRejectedAfterMove() throws Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));
    relationController.moveNoteToFolder(f.cells(), f.study());
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());

    byte[] staleProposal =
        proposalBundleBytes(
            binding(f.notebook()),
            List.of(new NotebookGitProposalFile("Study/Cells.md", EDITED_BODY)));
    ResponseStatusException rejection =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    f.notebook().getId(), acceptedA.getName(), staleProposal));

    assertThat(rejection.getStatusCode(), is(HttpStatus.CONFLICT));
    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedB));
  }

  /**
   * Builds a single-parent proposal on top of the accepted head in {@code acceptedBundleBytes}
   * (read through the notebook's own download endpoint by the caller, not {@code
   * binding.getBundleBytes()} directly - that column stops tracking the accepted head once a
   * binding's saves move onto native object storage) that preserves every file already in the
   * accepted tree and overrides exactly one path's content — mirroring a local checkout edit
   * followed by a commit on the moved note's new path.
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
