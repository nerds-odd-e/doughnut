package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

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
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      String downloadedSource =
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Moon.md");
      assertThat(downloadedSource, containsString("a part of"));
      assertThat(downloadedSource, containsString("[[Earth]]"));

      List<String> paths = GitBundleTestReader.pathsIn(repo, downloadedHead);
      assertThat(paths, not(hasItem(f.relation().getTitle() + ".md")));
      assertThat(paths, everyItem(not(startsWith("_trash/"))));
    }
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
