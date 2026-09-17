package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebRelationReduceControllerTest extends NotebookGitWebContentControllerTestBase {
  @Autowired RelationController relationController;

  @Test
  void reduceToSourcePropertyRecordsReducedSourceWithNoRelationshipOrTrashInAcceptedTree()
      throws Exception {
    RelationshipReduceFixture f = seedMoonEarthRelationshipForReduceToSource();

    relationController.reduceToSourceProperty(f.relation());

    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead = GitBundleTestReader.fetchHead(repo, downloadedBundle(f.notebook()));
      String downloadedSource =
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Moon.md");
      assertThat(downloadedSource, containsString("a part of"));
      assertThat(downloadedSource, containsString("[[Earth]]"));

      List<String> paths = GitBundleTestReader.pathsIn(repo, downloadedHead);
      assertThat(paths, not(hasItem(f.relation().getTitle() + ".md")));
      assertThat(paths, everyItem(not(startsWith("_trash/"))));
    }
  }

  @Test
  void reduceAcrossNotebooksAppendsOneAcceptedCommitToEachNotebookAndMovesTheTracker()
      throws Exception {
    Notebook spaceTopics = createGitBackedNotebook("Space topics");
    Notebook astronomy = createGitBackedNotebook("Astronomy");
    Note moon = makeMe.aNote("Moon").notebook(astronomy).please();
    makeMe.aNote("Earth").notebook(spaceTopics).please();
    Note relation =
        makeMe
            .aNote("Moon a part of Earth")
            .notebook(spaceTopics)
            .content(
                "---\n"
                    + "type: Relationship\n"
                    + "relation: a-part-of\n"
                    + "source: \"[[Astronomy:Moon]]\"\n"
                    + "target: \"[[Earth]]\"\n"
                    + "---\n")
            .please();
    MemoryTracker tracker = learnedTracker(relation, 0.3f);
    String spaceTopicsHeadBefore =
        snapshotCurrentPortableTree(spaceTopics).getAcceptedGitObjectId();
    String astronomyHeadBefore = snapshotCurrentPortableTree(astronomy).getAcceptedGitObjectId();

    relationController.reduceToSourceProperty(relation);

    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit astronomyCommit =
          GitBundleTestReader.fetchSingleParentCommit(repo, downloadedBundle(astronomy));
      assertThat(astronomyCommit.parent().name(), equalTo(astronomyHeadBefore));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, astronomyCommit.head(), "Moon.md"),
          containsString("[[Space topics:Earth|Earth]]"));
    }
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit spaceTopicsCommit =
          GitBundleTestReader.fetchSingleParentCommit(repo, downloadedBundle(spaceTopics));
      assertThat(spaceTopicsCommit.parent().name(), equalTo(spaceTopicsHeadBefore));
      List<String> paths = GitBundleTestReader.pathsIn(repo, spaceTopicsCommit.head());
      assertThat(paths, hasItem("Earth.md"));
      assertThat(paths, not(hasItem(relation.getTitle() + ".md")));
      assertThat(paths, everyItem(not(startsWith("_trash/"))));
    }
    MemoryTracker moved = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(moved.getNote().getId(), equalTo(moon.getId()));
    assertThat(moved.getPropertyKey(), equalTo("a part of"));
  }

  byte[] downloadedBundle(Notebook notebook) throws UnexpectedNoAccessRightException {
    return controller
        .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
        .getBody();
  }

  RelationshipReduceFixture seedMoonEarthRelationshipForReduceToSource()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Note source = makeMe.aNote("Moon").notebook(notebook).please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship("a part of", source, target)
            .please();
    snapshotCurrentPortableTree(notebook);
    return new RelationshipReduceFixture(notebook, relation);
  }

  record RelationshipReduceFixture(Notebook notebook, Note relation) {}
}
