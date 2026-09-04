package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Asserts that a freshly created notebook already has its accepted Git binding: one root commit on
 * {@code refs/heads/main} with no parents and an empty tree.
 */
final class NotebookGitBindingAssertions {

  private NotebookGitBindingAssertions() {}

  static void assertEmptyTreeRootCommitBinding(
      NotebookGitBindingRepository notebookGitBindingRepository, Integer notebookId)
      throws Exception {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, binding.getBundleBytes());
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));

      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(commit.getParentCount(), equalTo(0));

        List<String> blobPaths = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(readBack)) {
          treeWalk.addTree(commit.getTree());
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            blobPaths.add(treeWalk.getPathString());
          }
        }
        assertThat(blobPaths, empty());

        revWalk.reset();
        revWalk.markStart(commit);
        int commitCount = 0;
        for (RevCommit ignored : revWalk) {
          commitCount++;
        }
        assertThat(commitCount, equalTo(1));
      }
    }
  }
}
