package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Asserts that a freshly created notebook already has its accepted Git binding: one root commit on
 * {@code refs/heads/main} with no parents, LFS representation, and initial {@code .gitattributes}.
 * The accepted history is read through the notebook's own Git-bundle download endpoint.
 */
final class NotebookGitBindingAssertions {

  private NotebookGitBindingAssertions() {}

  static void assertInitialLfsRootCommitBinding(
      NotebookGitBindingRepository notebookGitBindingRepository,
      NotebookController notebookController,
      Notebook notebook)
      throws Exception {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(binding.getAttachmentRepresentation(), is(NotebookGitAttachmentRepresentation.LFS));
    byte[] acceptedBundle = notebookController.downloadNotebookGitBundle(notebook).getBody();

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, acceptedBundle);
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));

      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(commit.getParentCount(), equalTo(0));
        assertThat(
            GitBundleTestReader.readTreeEntriesWithMetadata(readBack, commit),
            contains(
                PortableTreeEntry.ofText(
                    NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT)));

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
