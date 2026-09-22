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
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

/**
 * Slice 4 linked-move proof: a same-notebook referrer carries an exact path link to the moved note
 * in both body and frontmatter. The web Move appends B; the accepted B tree carries the relocated
 * file at {@code Study/Cells.md} and the referrer file with the ordinary rewritten reference {@code
 * [[Study/Cells|shown]]} in both locations, preserving the visible label. Capture-before- placement
 * and rewrite-after-placement already run inside slice 2's accepted-history edit transaction; this
 * slice only adds the missing behavioral evidence.
 */
class NotebookGitWebNoteMoveLinkedReferrerControllerTest extends NotebookGitWebNoteMoveTestBase {

  static final String REFERRER_BODY_BEFORE =
      "---\ntype: Note\nrelated: \"[[Biology/Cells|shown]]\"\n---\nSee [[Biology/Cells|shown]] for "
          + "details.";
  static final String REFERRER_BODY_AFTER =
      "---\ntype: Note\nrelated: \"[[Study/Cells|shown]]\"\n---\nSee [[Study/Cells|shown]] for "
          + "details.";

  @Test
  void webMoveAppendsAcceptedTreeWithRewrittenInNotebookReferencesInBothLocations()
      throws UnexpectedNoAccessRightException, Exception {
    LearnedMoveFixture f = seedLearnedCellsWithLinkedReferrer();
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
          containsInAnyOrder(
              "Biology/README.md", "Study/README.md", "Study/Cells.md", "Reading.md"));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Biology/Cells.md")));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Study/Cells.md"),
          equalTo(CELLS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Reading.md"),
          equalTo(REFERRER_BODY_AFTER));
    }
  }

  /**
   * Slice 4 fixture: slice 2's learned {@code Biology/Cells.md} with a represented {@code Study}
   * destination, plus a same-notebook referrer "Reading" at the notebook root whose body and
   * frontmatter both carry the exact path link {@code [[Biology/Cells|shown]]}. Authored through
   * {@link #authorReferencingContent} so the inbound reference rows are populated and the move's
   * capture-before-placement finds the referrer.
   */
  private LearnedMoveFixture seedLearnedCellsWithLinkedReferrer()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Biology")
            .readmeContent("Biology readme")
            .please();
    Folder study =
        makeMe.aFolder().notebook(notebook).name("Study").readmeContent("Study readme").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    Note referrer = makeMe.aNote("Reading").notebook(notebook).please();
    authorReferencingContentCommitted(referrer, REFERRER_BODY_BEFORE);
    long recallCountBefore = countRecallPromptsByNoteId(cells.getId());
    snapshotCurrentPortableTree(notebook);
    return new LearnedMoveFixture(
        notebook, biology, study, cells, tracker, recallCountBefore, currentUser.getUser());
  }
}
