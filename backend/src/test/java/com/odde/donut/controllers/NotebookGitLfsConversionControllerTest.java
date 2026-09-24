package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitLfsConversionService;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Converting a notebook still on legacy raw attachment storage moves it to LFS with one forward
 * Donut System commit; a notebook already on LFS is left as it is.
 */
class NotebookGitLfsConversionControllerTest extends NotebookGitControllerTestBase {

  private static final String NOTE_CONTENT = "---\ntype: Note\n---\nkept as it is";

  @Autowired NotebookGitLfsConversionService conversionService;

  @Test
  void aRawNotebookWithoutFilesBecomesLfsInOneForwardCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(NOTE_CONTENT).please();
    String previousHead = snapshotCurrentPortableTree(notebook).getAcceptedGitObjectId();
    List<PortableTreeEntry> expectedEntries =
        new ArrayList<>(acceptedHistory(notebook).tipContent());
    expectedEntries.add(0, NotebookGitAttributes.initialEntry());

    conversionService.convert(notebook.getId(), Instant.now());

    assertThat(
        reloadCommittedBinding(notebook.getId()).getAttachmentRepresentation(),
        is(NotebookGitAttachmentRepresentation.LFS));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      RevCommit conversion = revWalk.parseCommit(head);
      assertThat(conversion.getParentCount(), equalTo(1));
      assertThat(conversion.getParent(0).name(), equalTo(previousHead));
      assertThat(
          conversion.getAuthorIdent().getName(),
          equalTo(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
      assertThat(
          conversion.getAuthorIdent().getEmailAddress(),
          equalTo(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
      assertThat(conversion.getFullMessage(), equalTo("Store notebook files with Git LFS"));
      assertThat(
          GitBundleTestReader.readTreeEntries(repository, conversion), equalTo(expectedEntries));
    }
  }

  @Test
  void convertingAgainLeavesTheHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    conversionService.convert(notebook.getId(), Instant.now());
    AcceptedHistory converted = acceptedHistory(notebook);

    conversionService.convert(notebook.getId(), Instant.now());

    assertThat(acceptedHistory(notebook), equalTo(converted));
  }

  @Test
  void anLfsNotebookIsLeftAsItIs() throws Exception {
    Notebook notebook = createProductLfsNotebook();
    AcceptedHistory before = acceptedHistory(notebook);

    conversionService.convert(notebook.getId(), Instant.now());

    assertThat(acceptedHistory(notebook), equalTo(before));
  }
}
