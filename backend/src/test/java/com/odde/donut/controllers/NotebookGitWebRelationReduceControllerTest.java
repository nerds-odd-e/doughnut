package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    Note relation = astronomyMoonAPartOfEarthRelationshipIn(spaceTopics);
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

  @Test
  void refusedReduceIntoReadOnlySourceNotebookLeavesBothNotebooksUnchanged() throws Exception {
    Notebook astronomy = createGitBackedNotebook("Astronomy");
    makeMe.aNote("Moon").notebook(astronomy).please();
    makeMe.aBazaarNotebook(astronomy).please();
    currentUser.setUser(createFixtureUser());
    Notebook spaceTopics = createGitBackedNotebook("Space topics");
    Note relation = astronomyMoonAPartOfEarthRelationshipIn(spaceTopics);
    String spaceTopicsHeadBefore =
        snapshotCurrentPortableTree(spaceTopics).getAcceptedGitObjectId();
    String astronomyHeadBefore = snapshotCurrentPortableTree(astronomy).getAcceptedGitObjectId();

    ResponseStatusException refused =
        assertThrows(
            ResponseStatusException.class,
            () -> relationController.reduceToSourceProperty(relation));

    assertThat(refused.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(refused.getReason(), equalTo("Could not resolve the relationship source note."));
    assertThat(binding(spaceTopics).getAcceptedGitObjectId(), equalTo(spaceTopicsHeadBefore));
    assertThat(binding(astronomy).getAcceptedGitObjectId(), equalTo(astronomyHeadBefore));
    assertThat(noteRepository.findById(relation.getId()).isPresent(), is(true));
  }

  byte[] downloadedBundle(Notebook notebook) throws UnexpectedNoAccessRightException {
    return controller
        .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
        .getBody();
  }

  /** Earth plus a relationship note in {@code spaceTopics} whose source is Astronomy's Moon. */
  Note astronomyMoonAPartOfEarthRelationshipIn(Notebook spaceTopics) {
    makeMe.aNote("Earth").notebook(spaceTopics).please();
    return makeMe
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
